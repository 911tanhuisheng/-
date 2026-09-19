import { nextTick, onBeforeUnmount, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { storeToRefs } from 'pinia'
import { bailianAssistChat, type BailianTurn } from '@/api/bailianAssist'
import type { BailianAssistChatRequest } from '@generated'
import { renderMarkdownToHtml } from '@/utils/renderMarkdown'
import { useStudyAiCopilotStore, type StudyAiMessage } from '@/stores/studyAiCopilot'

function shiftFirstCodePoint(s: string): [string, string] {
  if (!s) return ['', '']
  const cp = s.codePointAt(0)!
  const len = cp > 0xffff ? 2 : 1
  return [s.slice(0, len), s.slice(len)]
}

export function useStudyAiCopilotStream(
  buildBody: (userText: string, history: BailianTurn[]) => BailianAssistChatRequest,
) {
  const store = useStudyAiCopilotStore()
  const { messages, draft, sending, streamingAssistantId, streamingPlainText } = storeToRefs(store)

  let streamAbort: AbortController | null = null
  let scrollRaf: number | null = null
  let twPumpRaf: number | null = null
  let activeTwPumpSession: { alive: boolean } | null = null

  function stopTwPump() {
    if (activeTwPumpSession) {
      activeTwPumpSession.alive = false
      activeTwPumpSession = null
    }
    if (twPumpRaf != null) {
      cancelAnimationFrame(twPumpRaf)
      twPumpRaf = null
    }
  }

  function bumpAssistantHtml(m: StudyAiMessage) {
    if (m.role === 'assistant') {
      m.html = renderMarkdownToHtml(m.content)
    }
  }

  function pushUser(text: string) {
    const t = text.trim()
    if (!t) return
    messages.value.push({
      id: `u-${Date.now()}`,
      role: 'user',
      content: t,
    })
  }

  function toHistory(): BailianTurn[] {
    return messages.value.map((m) => ({ role: m.role, content: m.content }))
  }

  async function scrollToEnd(el: HTMLElement | null) {
    await nextTick()
    if (el) el.scrollTop = el.scrollHeight
  }

  function scrollListToBottomSync(el: HTMLElement | null) {
    if (el) el.scrollTop = el.scrollHeight
  }

  function scheduleScrollToEnd(el: HTMLElement | null) {
    if (scrollRaf != null) return
    scrollRaf = requestAnimationFrame(() => {
      scrollRaf = null
      scrollListToBottomSync(el)
    })
  }

  async function sendUserMessage(text: string, listEl: HTMLElement | null) {
    const raw = text.trim()
    if (!raw || sending.value) return
    pushUser(raw)
    draft.value = ''
    await scrollToEnd(listEl)
    sending.value = true

    streamAbort?.abort()
    streamAbort = new AbortController()
    const signal = streamAbort.signal
    stopTwPump()
    streamingPlainText.value = ''
    const pumpSession = { alive: true }
    activeTwPumpSession = pumpSession

    const hist = toHistory().slice(0, -1)

    const assistantMsg: StudyAiMessage = {
      id: `a-${Date.now()}`,
      role: 'assistant',
      content: '',
      html: '',
    }
    messages.value.push(assistantMsg)
    streamingAssistantId.value = assistantMsg.id
    await scrollToEnd(listEl)

    let twBuf = ''
    let twDeltaFlushQueued = false
    let twUpstreamClosed = false
    let twFinishedResolve: (() => void) | null = null
    const twFinished = new Promise<void>((r) => {
      twFinishedResolve = r
    })
    let twFinishNotified = false

    function notifyTwFinished() {
      if (twFinishNotified) return
      twFinishNotified = true
      twFinishedResolve?.()
      twFinishedResolve = null
    }

    function flushTwBufToMessage() {
      if (!twBuf) return
      assistantMsg.content += twBuf
      streamingPlainText.value = assistantMsg.content
      twBuf = ''
      scheduleScrollToEnd(listEl)
    }

    function pumpTypewriter() {
      twPumpRaf = null
      if (!pumpSession.alive) return

      if (twBuf.length === 0) {
        if (twUpstreamClosed) {
          bumpAssistantHtml(assistantMsg)
          scheduleScrollToEnd(listEl)
          notifyTwFinished()
        }
        return
      }

      const backlog = twBuf.length
      const perFrame = twUpstreamClosed
        ? Math.min(backlog, 72)
        : backlog > 320
          ? 20
          : backlog > 120
            ? 10
            : backlog > 36
              ? 4
              : 2

      let chunk = ''
      let rest = twBuf
      for (let i = 0; i < perFrame && rest.length > 0; i++) {
        const [ch, r] = shiftFirstCodePoint(rest)
        chunk += ch
        rest = r
      }
      twBuf = rest
      assistantMsg.content += chunk
      streamingPlainText.value = assistantMsg.content
      scheduleScrollToEnd(listEl)

      if (twBuf.length === 0 && twUpstreamClosed) {
        bumpAssistantHtml(assistantMsg)
        scheduleScrollToEnd(listEl)
        notifyTwFinished()
        return
      }

      if (twBuf.length > 0) {
        if (!pumpSession.alive) return
        twPumpRaf = requestAnimationFrame(pumpTypewriter)
      }
    }

    function kickTwPump() {
      if (!pumpSession.alive) return
      if (twPumpRaf != null) return
      twPumpRaf = requestAnimationFrame(pumpTypewriter)
    }

    try {
      await bailianAssistChat(buildBody(raw, hist), {
        onDelta: (piece) => {
          twBuf += piece
          if (!twDeltaFlushQueued) {
            twDeltaFlushQueued = true
            queueMicrotask(() => {
              twDeltaFlushQueued = false
              kickTwPump()
            })
          }
        },
        onError: (notice) => {
          stopTwPump()
          flushTwBufToMessage()
          twUpstreamClosed = true
          Message.error(notice || '请求失败')
          if (!assistantMsg.content.trim()) {
            assistantMsg.content = notice || '请求失败'
          } else {
            assistantMsg.content += `\n\n（${notice}）`
          }
          streamingPlainText.value = assistantMsg.content
          bumpAssistantHtml(assistantMsg)
          scheduleScrollToEnd(listEl)
          notifyTwFinished()
        },
        onDone: () => {
          twUpstreamClosed = true
          if (twBuf.length === 0) {
            stopTwPump()
            bumpAssistantHtml(assistantMsg)
            scheduleScrollToEnd(listEl)
            notifyTwFinished()
          } else {
            kickTwPump()
          }
        },
      }, { signal })
      await twFinished
    } catch (e) {
      stopTwPump()
      flushTwBufToMessage()
      twUpstreamClosed = true
      notifyTwFinished()
      if (e instanceof DOMException && e.name === 'AbortError') {
        if (!assistantMsg.content.trim()) {
          messages.value = messages.value.filter((m) => m.id !== assistantMsg.id)
        }
      } else {
        const msg = e instanceof Error ? e.message : '网络错误'
        Message.error(msg)
        if (!assistantMsg.content.trim()) {
          assistantMsg.content = `请求失败：${msg}`
          streamingPlainText.value = assistantMsg.content
          bumpAssistantHtml(assistantMsg)
        }
      }
    } finally {
      stopTwPump()
      streamingPlainText.value = ''
      if (assistantMsg.content.trim()) {
        bumpAssistantHtml(assistantMsg)
      }
      sending.value = false
      streamingAssistantId.value = null
      await scrollToEnd(listEl)
    }
  }

  function stopGeneration() {
    streamAbort?.abort()
  }

  onBeforeUnmount(() => {
    streamAbort?.abort()
    stopTwPump()
    if (scrollRaf != null) {
      cancelAnimationFrame(scrollRaf)
      scrollRaf = null
    }
  })

  return {
    sendUserMessage,
    stopGeneration,
    scrollToEnd,
    scheduleScrollToEnd,
    scrollListToBottomSync,
  }
}

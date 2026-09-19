import { OpenAPI } from '@generated'
import type { BailianAssistChatRequest, BailianAssistTurnDTO } from '@generated'
import { AI_ASSIST_PROFANITY_BANNED_CODE, isResultSuccess } from '@/api/result'
import { useAuthStore } from '@/stores/auth'

/** 与 OpenAPI 契约一致；历史轮次 role 为 string（常见为 user / assistant） */
export type BailianTurn = BailianAssistTurnDTO

export type BailianStreamHandlers = {
  onDelta: (text: string) => void
  onError: (notice: string) => void
  onDone: () => void
}

async function streamAuthHeaders(): Promise<Record<string, string>> {
  const tokenResolver = OpenAPI.TOKEN
  const token = typeof tokenResolver === 'function' ? await tokenResolver({} as never) : tokenResolver
  const h: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  }
  if (token?.trim()) h.Authorization = `Bearer ${token.trim()}`
  return h
}

/**
 * POST `/bailian_assist/chat` — SSE（event: delta | error | done）。
 * 使用 `fetch` + ReadableStream：codegen 的 `BailianAssistService.chat` 走 Axios，不适配流式响应体。
 */
export async function bailianAssistChat(
  body: BailianAssistChatRequest,
  handlers: BailianStreamHandlers,
  options?: { signal?: AbortSignal },
): Promise<void> {
  const base = OpenAPI.BASE.replace(/\/+$/, '')
  const res = await fetch(`${base}/bailian_assist/chat`, {
    method: 'POST',
    headers: await streamAuthHeaders(),
    body: JSON.stringify(body),
    signal: options?.signal,
    credentials: OpenAPI.WITH_CREDENTIALS ? 'include' : 'same-origin',
  })

  const contentType = (res.headers.get('content-type') || '').toLowerCase()
  const isEventStream = contentType.includes('text/event-stream')

  if (!res.ok || !res.body) {
    const errText = await res.text().catch(() => '')
    const parsed = parseApiResultBody(errText)
    if (parsed?.code === AI_ASSIST_PROFANITY_BANNED_CODE) {
      void useAuthStore().syncSessionStatus()
    }
    handlers.onError(parsed?.message || (res.status === 401 ? '请先登录' : `HTTP ${res.status}`))
    handlers.onDone()
    return
  }

  if (!isEventStream) {
    const text = await res.text()
    const parsed = parseApiResultBody(text)
    if (parsed?.code === AI_ASSIST_PROFANITY_BANNED_CODE) {
      void useAuthStore().syncSessionStatus()
    }
    handlers.onError(parsed?.message || '请求失败')
    handlers.onDone()
    return
  }

  const reader = res.body.getReader()
  const dec = new TextDecoder()
  let buf = ''
  let doneEmitted = false

  const safeDone = () => {
    if (doneEmitted) return
    doneEmitted = true
    handlers.onDone()
  }

  const flushBlock = (rawBlock: string) => {
    const block = rawBlock.replace(/\r/g, '').trimEnd()
    if (!block) return
    let eventName = 'message'
    const dataParts: string[] = []
    for (const ln of block.split('\n')) {
      const line = ln.trimEnd()
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataParts.push(line.slice(5).trimStart())
      }
    }
    const dataStr = dataParts.join('\n')
    if (!dataStr) return

    const parseDelta = (): boolean => {
      try {
        const o = JSON.parse(dataStr) as { t?: string }
        if (typeof o.t === 'string' && o.t.length > 0) {
          handlers.onDelta(o.t)
          return true
        }
      } catch {
        /* ignore */
      }
      return false
    }

    const parseError = (): boolean => {
      try {
        const o = JSON.parse(dataStr) as { notice?: string; code?: number }
        if (typeof o.notice === 'string') {
          if (o.code === AI_ASSIST_PROFANITY_BANNED_CODE) {
            void useAuthStore().syncSessionStatus()
          }
          handlers.onError(o.notice || '请求失败')
          return true
        }
      } catch {
        /* ignore */
      }
      return false
    }

    if (eventName === 'done') {
      safeDone()
      return
    }
    if (eventName === 'error') {
      if (!parseError()) {
        handlers.onError(dataStr)
      }
      return
    }
    if (eventName === 'delta' || eventName === 'message') {
      if (parseDelta()) return
      if (eventName === 'message' && parseError()) return
      return
    }
    if (parseDelta()) return
    if (parseError()) return
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += dec.decode(value, { stream: true })
      buf = buf.replace(/\r\n/g, '\n')
      let sep: number
      while ((sep = buf.indexOf('\n\n')) >= 0) {
        const block = buf.slice(0, sep)
        buf = buf.slice(sep + 2)
        flushBlock(block)
      }
    }
    buf = buf.replace(/\r\n/g, '\n')
    if (buf.trim()) {
      flushBlock(buf)
    }
  } catch (e) {
    if (!(e instanceof DOMException && e.name === 'AbortError')) {
      const msg = e instanceof Error ? e.message : '网络异常'
      handlers.onError(msg)
    }
  } finally {
    reader.releaseLock()
  }
  safeDone()
}

function parseApiResultBody(text: string): { code?: number; message?: string } | null {
  const raw = text.trim()
  if (!raw.startsWith('{')) return null
  try {
    const o = JSON.parse(raw) as { code?: number; message?: string }
    if (typeof o.code === 'number' && !isResultSuccess(o.code)) {
      return { code: o.code, message: o.message?.trim() || undefined }
    }
  } catch {
    /* ignore */
  }
  return null
}

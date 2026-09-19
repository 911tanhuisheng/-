<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useClipboard } from '@vueuse/core'
import { extractMarkdownCodeBlocks } from '@/utils/extractMarkdownCodeBlocks'

const props = defineProps<{
  message: {
    id: string
    role: 'user' | 'assistant'
    content: string
    html?: string
  }
  /** 当前流式中的气泡 */
  isStreaming: boolean
  /** 流式纯文本（仅 isStreaming 时） */
  streamingPlainText?: string
}>()

const emit = defineEmits<{
  'insert-code': [code: string]
}>()

const mdRoot = ref<HTMLElement | null>(null)
const { copy, isSupported } = useClipboard({ legacy: true })
const copiedIdx = ref<number | null>(null)

const variant = computed(() => {
  if (props.message.role === 'user') return 'user' as const
  const c = props.message.content
  if (/错误|失败|Error|HTTP\s\d|异常|请求失败/.test(c)) return 'error' as const
  if (/```/.test(c)) return 'code' as const
  if (/思路|提示|建议|注意/.test(c)) return 'hint' as const
  return 'analysis' as const
})

const barClass = computed(() => {
  switch (variant.value) {
    case 'user':
      return 'tw-bg-zinc-400 dark:tw-bg-zinc-500'
    case 'hint':
      return 'tw-bg-violet-500'
    case 'code':
      return 'tw-bg-emerald-500'
    case 'error':
      return 'tw-bg-rose-500'
    default:
      return 'tw-bg-sky-500'
  }
})

const fences = computed(() =>
  props.message.role === 'assistant' ? extractMarkdownCodeBlocks(props.message.content) : [],
)

let highlightIdle = 0
let highlightCancelled = false

async function highlightCodeBlocksWithPrism() {
  if (props.message.role !== 'assistant' || props.isStreaming || !props.message.html) return
  const root = mdRoot.value
  if (!root) return

  const run = async () => {
    if (highlightCancelled) return
    const PrismMod = await import('prismjs')
    await import('prismjs/themes/prism.css')
    const Prism = PrismMod.default
    await import('prismjs/components/prism-clike')
    await import('prismjs/components/prism-c')
    await import('prismjs/components/prism-cpp')
    await import('prismjs/components/prism-java')
    await import('prismjs/components/prism-python')
    await import('prismjs/components/prism-javascript')

    const pres = root.querySelectorAll<HTMLElement>('pre > code')
    for (const codeEl of pres) {
      if (highlightCancelled) return
      const pre = codeEl.parentElement
      if (!pre || pre.dataset.prismDone === '1') continue
      const code = codeEl.textContent ?? ''
      if (!code.trim()) continue
      try {
        Prism.highlightElement(codeEl)
        pre.dataset.prismDone = '1'
      } catch {
        pre.dataset.prismDone = '1'
      }
    }
  }

  if (typeof requestIdleCallback !== 'undefined') {
    highlightIdle = requestIdleCallback(() => void run(), { timeout: 1200 })
  } else {
    await nextTick()
    setTimeout(() => void run(), 0)
  }
}

watch(
  () => [props.message.html, props.message.id, props.isStreaming] as const,
  async () => {
    highlightCancelled = true
    if (typeof cancelIdleCallback !== 'undefined' && highlightIdle) cancelIdleCallback(highlightIdle)
    highlightCancelled = false
    await nextTick()
    void highlightCodeBlocksWithPrism()
  },
  { flush: 'post' },
)

onBeforeUnmount(() => {
  highlightCancelled = true
  if (typeof cancelIdleCallback !== 'undefined' && highlightIdle) cancelIdleCallback(highlightIdle)
})

async function onCopyBlock(code: string, idx: number) {
  if (!isSupported.value) return
  await copy(code)
  copiedIdx.value = idx
  window.setTimeout(() => {
    if (copiedIdx.value === idx) copiedIdx.value = null
  }, 1600)
}

function onInsert(code: string) {
  emit('insert-code', code)
}
</script>

<template>
  <article
    class="tw-group tw-relative tw-flex tw-gap-2.5 tw-rounded-2xl tw-border tw-border-black/[0.06] tw-bg-white/80 tw-px-3 tw-py-2.5 tw-pl-2 tw-shadow-md tw-shadow-black/5 tw-backdrop-blur-sm tw-transition-shadow hover:tw-shadow-lg dark:tw-border-white/10 dark:tw-bg-zinc-900/75 dark:tw-shadow-black/30"
  >
    <span
      class="tw-mt-0.5 tw-inline-block tw-w-1 tw-shrink-0 tw-self-stretch tw-rounded-full"
      :class="barClass"
      aria-hidden="true"
    />
    <div class="tw-min-w-0 tw-flex-1">
      <div class="tw-mb-1 tw-flex tw-items-center tw-gap-2">
        <span
          class="tw-inline-flex tw-h-7 tw-w-7 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-text-[11px] tw-font-bold tw-text-white tw-shadow-sm"
          :class="
            message.role === 'user'
              ? 'tw-bg-gradient-to-br tw-from-zinc-500 tw-to-zinc-700'
              : isStreaming
                ? 'tw-animate-pulse tw-bg-gradient-to-br tw-from-teal-500 tw-to-cyan-600'
                : 'tw-bg-gradient-to-br tw-from-teal-500 tw-to-cyan-600'
          "
        >
          {{ message.role === 'user' ? '我' : 'AI' }}
        </span>
        <span class="tw-text-[11px] tw-font-medium tw-text-zinc-500 dark:tw-text-zinc-400">
          {{ message.role === 'user' ? '你' : variant === 'error' ? '提示' : '助手' }}
        </span>
      </div>

      <template v-if="message.role === 'assistant'">
        <div
          v-if="isStreaming && !message.content.trim()"
          class="tw-space-y-2 tw-py-1"
          aria-live="polite"
        >
          <div class="tw-h-2.5 tw-w-[92%] tw-max-w-md tw-rounded-md tw-bg-gradient-to-r tw-from-zinc-200 tw-via-zinc-100 tw-to-zinc-200 tw-bg-[length:200%_100%] tw-animate-ai-shimmer dark:tw-from-zinc-700 dark:tw-via-zinc-600 dark:tw-to-zinc-700" />
          <div class="tw-h-2.5 tw-w-[78%] tw-max-w-sm tw-rounded-md tw-bg-gradient-to-r tw-from-zinc-200 tw-via-zinc-100 tw-to-zinc-200 tw-bg-[length:200%_100%] tw-animate-ai-shimmer dark:tw-from-zinc-700 dark:tw-via-zinc-600 dark:tw-to-zinc-700" />
          <div class="tw-h-2.5 tw-w-[64%] tw-max-w-xs tw-rounded-md tw-bg-gradient-to-r tw-from-zinc-200 tw-via-zinc-100 tw-to-zinc-200 tw-bg-[length:200%_100%] tw-animate-ai-shimmer dark:tw-from-zinc-700 dark:tw-via-zinc-600 dark:tw-to-zinc-700" />
        </div>
        <div v-else-if="isStreaming" class="tw-whitespace-pre-wrap tw-break-words tw-text-sm tw-leading-relaxed tw-text-zinc-800 dark:tw-text-zinc-100">
          {{ streamingPlainText }}<span class="tw-inline-block tw-w-2 tw-animate-ai-type-caret tw-align-text-bottom">▍</span>
        </div>
        <div v-else>
          <div ref="mdRoot" class="ai-msg-md oj-statement tw-text-sm tw-leading-relaxed tw-text-zinc-800 dark:tw-text-zinc-100" v-html="message.html" />
          <div v-if="fences.length" class="tw-mt-2 tw-flex tw-flex-col tw-gap-2">
            <div
              v-for="(f, i) in fences"
              :key="i"
              class="tw-flex tw-flex-wrap tw-items-center tw-gap-2"
            >
              <button
                type="button"
                class="tw-rounded-lg tw-border tw-border-teal-500/35 tw-bg-teal-500/10 tw-px-2.5 tw-py-1 tw-text-[11px] tw-font-semibold tw-text-teal-800 tw-transition hover:tw-bg-teal-500/20 dark:tw-text-teal-200"
                @click="onInsert(f.code)"
              >
                插入代码块 {{ i + 1 }}
              </button>
              <button
                type="button"
                class="tw-rounded-lg tw-border tw-border-zinc-300 tw-bg-white/60 tw-px-2.5 tw-py-1 tw-text-[11px] tw-font-semibold tw-text-zinc-700 tw-backdrop-blur hover:tw-bg-white dark:tw-border-zinc-600 dark:tw-bg-zinc-800/60 dark:tw-text-zinc-200"
                @click="onCopyBlock(f.code, i)"
              >
                {{ copiedIdx === i ? '已复制' : '复制' }}
              </button>
            </div>
          </div>
        </div>
      </template>
      <p v-else class="tw-m-0 tw-whitespace-pre-wrap tw-break-words tw-text-sm tw-leading-relaxed tw-text-zinc-800 dark:tw-text-zinc-100">
        {{ message.content }}
      </p>
    </div>
  </article>
</template>

<style scoped>
.ai-msg-md :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  border-radius: 10px;
  font-size: 12px;
}
.ai-msg-md :deep(code[class*='language-']) {
  font-family: ui-monospace, 'JetBrains Mono', Consolas, monospace;
}
</style>

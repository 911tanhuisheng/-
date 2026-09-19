<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { nextTick } from 'vue'
import { useStudyAiCopilotStore } from '@/stores/studyAiCopilot'

export type QuickChip = { label: string; text: string; icon?: string }

const props = defineProps<{
  quickActions: QuickChip[]
  disabled: boolean
}>()

const emit = defineEmits<{
  send: []
  stop: []
}>()

const store = useStudyAiCopilotStore()
const { draft, sending } = storeToRefs(store)

function onSend() {
  emit('send')
}

function pickQuick(c: QuickChip) {
  draft.value = c.text
  void nextTick(() => emit('send'))
}
</script>

<template>
  <div class="tw-flex tw-flex-col tw-gap-2">
    <div
      class="tw-no-scrollbar tw-flex tw-gap-2 tw-overflow-x-auto tw-pb-0.5 tw-pl-0.5"
      style="-webkit-overflow-scrolling: touch"
    >
      <button
        v-for="(c, i) in quickActions"
        :key="i"
        type="button"
        class="tw-shrink-0 tw-rounded-full tw-border tw-border-teal-500/15 tw-bg-teal-500/[0.06] tw-px-3 tw-py-1.5 tw-text-[11px] tw-font-semibold tw-text-zinc-700 tw-transition hover:-tw-translate-y-0.5 hover:tw-border-teal-400/50 hover:tw-bg-teal-500/10 disabled:tw-opacity-40 dark:tw-border-teal-400/15 dark:tw-bg-teal-400/[0.07] dark:tw-text-zinc-100"
        :disabled="disabled"
        @click="pickQuick(c)"
      >
        <span v-if="c.icon" class="tw-mr-1">{{ c.icon }}</span>{{ c.label }}
      </button>
    </div>

    <div class="tw-flex tw-items-end tw-gap-2 tw-rounded-2xl tw-border tw-border-black/[0.06] tw-bg-white/70 tw-p-2 tw-shadow-[0_8px_24px_rgba(15,118,110,0.08)] dark:tw-border-white/10 dark:tw-bg-zinc-900/70">
      <textarea
        v-model="draft"
        rows="2"
        class="tw-min-h-[56px] tw-flex-1 tw-resize-none tw-border-0 tw-bg-transparent tw-px-2 tw-py-2 tw-text-sm tw-text-zinc-900 tw-outline-none tw-ring-0 placeholder:tw-text-zinc-400 disabled:tw-opacity-50 dark:tw-text-zinc-50 dark:placeholder:tw-text-zinc-500"
        placeholder="描述你的疑问，Enter 发送（Shift+Enter 换行）…"
        :disabled="disabled"
        @keydown.enter.exact.prevent="onSend"
      />
      <button
        v-if="sending"
        type="button"
        class="tw-shrink-0 tw-rounded-xl tw-border tw-border-rose-300 tw-bg-rose-500/15 tw-px-3 tw-py-2 tw-text-xs tw-font-bold tw-text-rose-700 dark:tw-border-rose-500/40 dark:tw-text-rose-200"
        @click="emit('stop')"
      >
        停止
      </button>
      <button
        type="button"
        class="tw-grid tw-h-10 tw-w-10 tw-shrink-0 tw-place-items-center tw-rounded-xl tw-bg-gradient-to-br tw-from-teal-500 tw-to-cyan-600 tw-text-base tw-font-bold tw-text-white tw-shadow-lg tw-shadow-teal-500/25 tw-transition hover:-tw-translate-y-0.5 disabled:tw-cursor-not-allowed disabled:tw-opacity-40"
        :disabled="disabled || !draft.trim()"
        @click="onSend"
      >
        ↑
      </button>
    </div>
  </div>
</template>

<style scoped>
.tw-no-scrollbar::-webkit-scrollbar {
  display: none;
}
.tw-no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>

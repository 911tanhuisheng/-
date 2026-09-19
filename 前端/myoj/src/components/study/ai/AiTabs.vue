<script setup lang="ts">
import { computed, inject } from 'vue'
import AiHeroIcon from './AiHeroIcon.vue'
import { AI_COPILOT_ACTIVE_TAB_KEY, type AiCopilotTabId } from './keys'

const activeTab = inject(AI_COPILOT_ACTIVE_TAB_KEY)!
if (!activeTab) throw new Error('AiTabs: missing AI_COPILOT_ACTIVE_TAB_KEY')

const tabs = computed(() =>
  [
    { id: 'chat' as const, label: '对话', icon: 'chat-bubble-left-right' as const },
    { id: 'templates' as const, label: '快捷', icon: 'bolt' as const },
  ].map((t) => ({ ...t })),
)

function select(id: AiCopilotTabId) {
  activeTab.value = id
}

const activeIndex = computed(() => tabs.value.findIndex((t) => t.id === activeTab.value))
const indicatorStyle = computed(() => {
  const i = Math.max(0, activeIndex.value)
  const pct = tabs.value.length ? 100 / tabs.value.length : 50
  return {
    width: `${pct}%`,
    transform: `translateX(${i * 100}%)`,
  }
})
</script>

<template>
  <div class="tw-relative tw-rounded-xl tw-border tw-border-white/25 tw-bg-white/40 tw-p-1 tw-backdrop-blur-sm dark:tw-border-white/10 dark:tw-bg-zinc-900/50">
    <div
      class="tw-pointer-events-none tw-absolute tw-inset-y-1 tw-left-1 tw-rounded-lg tw-bg-gradient-to-r tw-from-teal-500/90 tw-to-cyan-600/90 tw-shadow-md tw-transition-transform tw-duration-300 tw-ease-out dark:tw-from-teal-400/85 dark:tw-to-cyan-500/85"
      :style="indicatorStyle"
    />
    <div class="tw-relative tw-z-10 tw-grid tw-grid-cols-2 tw-gap-0">
      <button
        v-for="t in tabs"
        :key="t.id"
        type="button"
        class="tw-flex tw-items-center tw-justify-center tw-gap-1.5 tw-rounded-lg tw-py-2 tw-text-xs tw-font-semibold tw-transition-colors tw-duration-200"
        :class="
          activeTab === t.id
            ? 'tw-text-white'
            : 'tw-text-zinc-600 hover:tw-text-zinc-900 dark:tw-text-zinc-300 dark:hover:tw-text-white'
        "
        @click="select(t.id)"
      >
        <AiHeroIcon :name="t.icon" />
        {{ t.label }}
      </button>
    </div>
  </div>
</template>

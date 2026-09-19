import type { InjectionKey, Ref } from 'vue'

export type AiCopilotTabId = 'chat' | 'templates'

export const AI_COPILOT_ACTIVE_TAB_KEY: InjectionKey<Ref<AiCopilotTabId>> = Symbol('aiCopilotActiveTab')

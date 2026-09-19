import { defineStore } from 'pinia'
import { ref } from 'vue'

export type StudyAiMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  html?: string
}

export const useStudyAiCopilotStore = defineStore('studyAiCopilot', () => {
  const sessionQuestionId = ref('')
  const messages = ref<StudyAiMessage[]>([])
  const draft = ref('')
  const sending = ref(false)
  const streamingAssistantId = ref<string | null>(null)
  const streamingPlainText = ref('')

  function clearChat() {
    messages.value = []
    streamingPlainText.value = ''
    draft.value = ''
  }

  /** 换题或重开面板时重置会话，避免题目间串话 */
  function bindSession(questionId: string) {
    if (sessionQuestionId.value === questionId) return
    sessionQuestionId.value = questionId
    clearChat()
  }

  return {
    sessionQuestionId,
    messages,
    draft,
    sending,
    streamingAssistantId,
    streamingPlainText,
    clearChat,
    bindSession,
  }
})

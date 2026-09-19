<script setup lang="ts">
import AiPanel from '@/components/study/ai/AiPanel.vue'

const open = defineModel<boolean>('open', { default: false })

const props = withDefaults(
  defineProps<{
    questionId: string
    questionTitle: string
    questionContent: string
    language: string
    questionType: string
    judgeContext: string
    sampleCases: string
    runResult: string
    getCode: () => string
  }>(),
  {
    questionId: '',
    questionTitle: '',
    questionContent: '',
    language: 'cpp',
    questionType: 'TEXT',
    judgeContext: '',
    sampleCases: '',
    runResult: '',
  },
)

const emit = defineEmits<{
  'insert-code': [code: string]
}>()
</script>

<template>
  <AiPanel
    v-model:open="open"
    :question-id="props.questionId"
    :question-title="props.questionTitle"
    :question-content="props.questionContent"
    :language="props.language"
    :question-type="props.questionType"
    :judge-context="props.judgeContext"
    :sample-cases="props.sampleCases"
    :run-result="props.runResult"
    :get-code="props.getCode"
    @insert-code="emit('insert-code', $event)"
  />
</template>

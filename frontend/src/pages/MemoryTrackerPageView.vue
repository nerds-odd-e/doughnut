<template>
  <div v-if="recallPrompts.length === 0" class="daisy-alert daisy-alert-info">
    No recall prompts found for this memory tracker.
  </div>
  <div v-else>
    <div v-if="firstPromptNote" class="daisy-mb-6">
      <NoteUnderQuestion v-bind="{ noteTopology: firstPromptNote.noteTopology }" />
    </div>
    <div
      v-for="prompt in recallPrompts"
      :key="prompt.id"
      class="daisy-card daisy-shadow-sm daisy-mb-4"
    >
      <div class="daisy-card-body">
        <div v-if="prompt.answerTime" class="daisy-text-sm daisy-text-base-content/70 daisy-mb-2">
          Answered: {{ new Date(prompt.answerTime).toLocaleString() }}
        </div>
        <div v-else class="daisy-text-sm daisy-text-base-content/70 daisy-mb-2">
          Unanswered
        </div>
        <QuestionDisplay
          v-if="prompt.predefinedQuestion && prompt.answer"
          v-bind="{
            multipleChoicesQuestion: prompt.predefinedQuestion.multipleChoicesQuestion,
            correctChoiceIndex: prompt.predefinedQuestion.correctAnswerIndex,
            answer: prompt.answer,
            disabled: true,
          }"
        />
        <QuestionDisplay
          v-else
          v-bind="{
            multipleChoicesQuestion: prompt.multipleChoicesQuestion,
            disabled: true,
          }"
        />
        <AnswerResult
          v-if="prompt.answer && prompt.predefinedQuestion && prompt.note"
          v-bind="{
            answeredQuestion: {
              answer: prompt.answer,
              predefinedQuestion: prompt.predefinedQuestion,
              note: prompt.note,
              recallPromptId: prompt.id,
              answerDisplay: getAnswerDisplay(prompt),
            },
          }"
        />
        <ConversationButton
          v-if="prompt.answer"
          :recall-prompt-id="prompt.id"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import type { RecallPrompt } from "@generated/backend"
import type { PropType } from "vue"
import NoteUnderQuestion from "@/components/review/NoteUnderQuestion.vue"
import QuestionDisplay from "@/components/review/QuestionDisplay.vue"
import AnswerResult from "@/components/review/AnswerResult.vue"
import ConversationButton from "@/components/review/ConversationButton.vue"

const props = defineProps({
  recallPrompts: {
    type: Array as PropType<RecallPrompt[]>,
    required: true,
  },
})

const firstPromptNote = computed(() => {
  return props.recallPrompts[0]?.note
})

const getAnswerDisplay = (prompt: RecallPrompt): string => {
  if (!prompt.answer) {
    return ""
  }
  const question = prompt.predefinedQuestion?.multipleChoicesQuestion ?? prompt.multipleChoicesQuestion
  if (!question) {
    return ""
  }
  const choiceIndex = prompt.answer.choiceIndex
  if (choiceIndex !== undefined && question.f1__choices) {
    return question.f1__choices[choiceIndex] || ""
  }
  return ""
}
</script>


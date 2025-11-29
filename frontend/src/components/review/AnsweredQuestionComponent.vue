<template>
  <div v-if="note">
    <NoteUnderQuestion v-bind="{ noteTopology: note.noteTopology }" />
  </div>
  <QuestionDisplay
    v-if="answeredQuestion.predefinedQuestion"
    v-bind="{
      multipleChoicesQuestion: answeredQuestion.predefinedQuestion.multipleChoicesQuestion,
      correctChoiceIndex: answeredQuestion.predefinedQuestion.correctAnswerIndex,
      answer: answeredQuestion.answer,
    }"
  />
  <AnswerResult v-bind="{ answeredQuestion }" />
  <ConversationButton
    v-if="conversationButton"
    :recall-prompt-id="answeredQuestion.recallPromptId"
  />
</template>

<script setup lang="ts">
import type { AnsweredQuestion } from "@generated/backend"
import type { PropType } from "vue"
import QuestionDisplay from "./QuestionDisplay.vue"
import ConversationButton from "./ConversationButton.vue"
import AnswerResult from "./AnswerResult.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"

const { answeredQuestion, conversationButton } = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
  conversationButton: {
    type: Boolean,
    required: true,
  },
})

const note = answeredQuestion?.note
</script>

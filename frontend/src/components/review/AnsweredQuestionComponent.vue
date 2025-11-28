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
    :question-id="answeredQuestion.recallPromptId"
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

<style lang="sass" scoped>
.note-under-question
  border: 1px solid #ccc
  width: 100%
  border-radius: 5px
  padding: 8px 12px
  margin-top: 15px
  background-color: #f9f9f9
  border: 2px solid #e0e0e0
  position: relative

  legend
    padding: 0 10px
    font-weight: 500
    color: #666
    font-size: 0.9rem
    background-color: #f9f9f9
    border-radius: 3px
    position: absolute
    top: -12px
    left: 10px
    width: auto
</style>

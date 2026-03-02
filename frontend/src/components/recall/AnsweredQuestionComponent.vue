<template>
  <div v-if="note">
    <NoteUnderQuestion v-bind="{ noteTopology: note.noteTopology }" />
    <ViewMemoryTrackerLink
      v-if="answeredQuestion.memoryTrackerId"
      :memory-tracker-id="answeredQuestion.memoryTrackerId"
    />
  </div>
  <QuestionDisplay
    v-if="answeredQuestion.recallPrompt?.predefinedQuestion"
    v-bind="{
      multipleChoicesQuestion: answeredQuestion.recallPrompt.predefinedQuestion!.multipleChoicesQuestion,
      correctChoiceIndex: answeredQuestion.recallPrompt.predefinedQuestion!.correctAnswerIndex,
      answer: answeredQuestion.answer,
    }"
  />
  <ConversationButton
    v-if="conversationButton && answeredQuestion.recallPrompt"
    :recall-prompt-id="answeredQuestion.recallPrompt.id"
  />
</template>

<script setup lang="ts">
import type { AnsweredQuestion } from "@generated/backend"
import type { PropType } from "vue"
import { computed } from "vue"
import QuestionDisplay from "./QuestionDisplay.vue"
import ConversationButton from "./ConversationButton.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"

const props = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
  conversationButton: {
    type: Boolean,
    required: true,
  },
})

const note = computed(() => props.answeredQuestion?.note)
</script>

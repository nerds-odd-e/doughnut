<template>
  <div v-if="answeredQuestion.recalledNote">
    <NoteUnderQuestion
      v-bind="{
        noteTopology: answeredQuestion.recalledNote.noteTopology,
        ancestorFolders: answeredQuestion.recalledNote.ancestorFolders ?? [],
        breadcrumbNotebookId: answeredQuestion.recalledNote.notebookId,
        focusedPropertyKey: answeredQuestion.recalledNote.propertyKey,
      }"
    />
    <ViewMemoryTrackerLink
      :memory-tracker-id="answeredQuestion.memoryTrackerId"
    />
  </div>
  <QuestionDisplay
    v-if="answeredQuestion.predefinedQuestion"
    v-bind="{
      multipleChoicesQuestion: answeredQuestion.predefinedQuestion.multipleChoicesQuestion,
      correctChoiceIndex: answeredQuestion.predefinedQuestion.correctAnswerIndex,
      answer: answeredQuestion.answer,
      testedFocus: answeredQuestion.predefinedQuestion.testedFocus,
      validationRationale: answeredQuestion.predefinedQuestion.validationRationale,
    }"
  />
  <ConversationButton
    v-if="conversationButton"
    :recall-prompt-id="answeredQuestion.id"
  />
</template>

<script setup lang="ts">
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import QuestionDisplay from "./QuestionDisplay.vue"
import ConversationButton from "./ConversationButton.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"

defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
  conversationButton: {
    type: Boolean,
    required: true,
  },
})
</script>

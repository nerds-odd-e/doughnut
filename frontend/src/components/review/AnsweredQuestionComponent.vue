<template>
  <div v-if="note">
    <div
      v-if="!toggleMemoryTracker"
      class="review-point-abbr"
      @click="toggleMemoryTracker = true"
    >
      <label class="me-1"><strong>Note reviewed: </strong></label>
      <NoteTopicComponent v-bind="{ noteTopic: note.noteTopic }" />
    </div>
    <div v-else>
      <NoteWithBreadcrumb
        v-bind="{ note, storageAccessor }"
      />
    </div>
  </div>
  <QuestionDisplay
    v-if="answeredQuestion.predefinedQuestion"
    v-bind="{
      bareQuestion: answeredQuestion.predefinedQuestion.bareQuestion,
      correctChoiceIndex: answeredQuestion.predefinedQuestion.correctAnswerIndex,
      answer: answeredQuestion.answer,
    }"
  />
  <AnswerResult v-bind="{ answeredQuestion }" />
</template>

<script setup lang="ts">
import type { AnsweredQuestion } from "@/generated/backend"
import type { PropType } from "vue"
import { ref } from "vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import QuestionDisplay from "./QuestionDisplay.vue"

const { answeredQuestion } = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const toggleMemoryTracker = ref(false)
const note = answeredQuestion?.note
</script>

<style lang="sass" scoped>
.review-point-abbr
  border: 1px solid #ccc
  width: 100%
  border-radius: 5px
  padding: 2px
</style>

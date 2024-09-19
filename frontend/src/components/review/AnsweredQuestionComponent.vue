<template>
  <div v-if="note">
    <div
      v-if="!toggleReviewPoint"
      class="review-point-abbr"
      @click="toggleReviewPoint = true"
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
import { computed, ref } from "vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import QuestionDisplay from "./QuestionDisplay.vue"

const props = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const toggleReviewPoint = ref(false)
const note = computed(() => props.answeredQuestion?.reviewPoint?.note)
</script>

<style lang="sass" scoped>
.review-point-abbr
  border: 1px solid #ccc
  width: 100%
  border-radius: 5px
  padding: 2px
</style>

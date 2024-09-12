<template>
  <div v-if="reviewPoint">
    <div
      v-if="!toggleReviewPoint"
      class="review-point-abbr"
      @click="toggleReviewPoint = true"
    >
      <label class="me-1"><strong>Review Point: </strong></label>
      <NoteTopicComponent v-bind="{ noteTopic: reviewPoint.note.noteTopic }" />
    </div>
    <div v-else>
      <NoteWithBreadcrumb
        v-bind="{ note: reviewPoint.note, storageAccessor }"
      />
      <NoteInfoReviewPoint v-model="reviewPoint" />
    </div>
  </div>
  <QuizQuestionDisplay
    v-if="answeredQuestion.predefinedQuestion"
    v-bind="{
      quizQuestion: answeredQuestion.predefinedQuestion.quizQuestion1,
      correctChoiceIndex: answeredQuestion.correctChoiceIndex,
      answerChoiceIndex: answeredQuestion.choiceIndex,
    }"
  />
  <AnswerResult v-bind="{ answeredQuestion }" />
</template>

<script setup lang="ts">
import { AnsweredQuestion } from "@/generated/backend"
import { PropType, computed, ref } from "vue"
import { StorageAccessor } from "../../store/createNoteStorage"
import QuizQuestionDisplay from "./QuizQuestionDisplay.vue"

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
const reviewPoint = computed(() => props.answeredQuestion?.reviewPoint)
</script>

<style lang="sass" scoped>
.review-point-abbr
  border: 1px solid #ccc
  width: 100%
  border-radius: 5px
  padding: 2px
</style>

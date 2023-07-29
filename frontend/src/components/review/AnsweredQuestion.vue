<template>
  <AnswerResult v-bind="{ answeredQuestion }" />
  <div v-if="reviewPoint">
    <ReviewPointAbbr
      v-if="!toggleReviewPoint"
      v-bind="{ reviewPoint }"
      @click="toggleReviewPoint = true"
    />
    <div v-else>
      <ShowReviewPoint v-bind="{ reviewPoint, storageAccessor }" />
      <NoteInfoReviewPoint
        v-bind="{ reviewPoint }"
        @self-evaluated="$emit('self-evaluated', $event)"
      />
    </div>
  </div>
  <QuizQuestion
    v-if="answeredQuestion.quizQuestion"
    v-bind="{
      quizQuestion: answeredQuestion.quizQuestion,
      correctChoiceIndex: answeredQuestion.correctChoiceIndex,
      answerChoiceIndex: answeredQuestion.choiceIndex,
    }"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteInfoReviewPoint from "@/components/notes/NoteInfoReviewPoint.vue";
import AnswerResult from "./AnswerResult.vue";
import QuizQuestion from "./QuizQuestion.vue";
import ShowReviewPoint from "./ShowReviewPoint.vue";
import ReviewPointAbbr from "./ReviewPointAbbr.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    answeredQuestion: {
      type: Object as PropType<Generated.AnsweredQuestion>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["self-evaluated"],
  components: {
    AnswerResult,
    QuizQuestion,
    ShowReviewPoint,
    NoteInfoReviewPoint,
    ReviewPointAbbr,
  },
  data() {
    return {
      toggleReviewPoint: false,
    };
  },
  computed: {
    reviewPoint() {
      return this.answeredQuestion?.reviewPoint;
    },
  },
});
</script>

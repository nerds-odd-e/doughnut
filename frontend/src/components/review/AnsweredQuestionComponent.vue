<template>
  <div v-if="reviewPoint">
    <NoteAbbr
      v-if="!toggleReviewPoint"
      v-bind="{ note: reviewPoint.note }"
      @click="toggleReviewPoint = true"
    />
    <div v-else>
      <ShowThing v-bind="{ note: reviewPoint.note, storageAccessor }" />
      <NoteInfoReviewPoint v-model="reviewPoint" />
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
  <AnswerResult v-bind="{ answeredQuestion }" />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteInfoReviewPoint from "@/components/notes/NoteInfoReviewPoint.vue";
import { AnsweredQuestion } from "@/generated/backend";
import AnswerResult from "./AnswerResult.vue";
import QuizQuestion from "./QuizQuestion.vue";
import ShowThing from "./ShowThing.vue";
import NoteAbbr from "./NoteAbbr.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    answeredQuestion: {
      type: Object as PropType<AnsweredQuestion>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    AnswerResult,
    QuizQuestion,
    ShowThing,
    NoteInfoReviewPoint,
    NoteAbbr,
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

<template>
  <div v-if="reviewPoint">
    <ThingAbbr
      v-if="!toggleReviewPoint"
      v-bind="{ thing: reviewPoint.thing }"
      @click="toggleReviewPoint = true"
    />
    <div v-else>
      <ShowThing v-bind="{ thing: reviewPoint.thing, storageAccessor }" />
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
import ThingAbbr from "./ThingAbbr.vue";
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
    ThingAbbr,
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

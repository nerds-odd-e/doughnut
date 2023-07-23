<template>
  <div class="container">
    <LoadingPage v-bind="{ contentExists: !!answeredQuestion }">
      <AnswerResult v-if="answeredQuestion" v-bind="{ answeredQuestion }" />

      <RadioButtons
        v-model="showQuiz"
        :options="[
          { value: 'review point', label: 'Review Point' },
          { value: 'quiz', label: 'Quiz' },
        ]"
      />
      <div v-if="reviewPoint">
        <ShowReviewPoint v-bind="{ reviewPoint, storageAccessor }" />
        <NoteInfoReviewPoint
          v-bind="{ reviewPoint }"
          @self-evaluated="onSelfEvaluated($event)"
        />
      </div>
      <QuizQuestion
        v-if="answeredQuestion?.quizQuestion"
        v-bind="{
          quizQuestion: answeredQuestion?.quizQuestion,
          reviewPointId: reviewPoint?.id,
          storageAccessor,
        }"
      />
    </LoadingPage>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteInfoReviewPoint from "@/components/notes/NoteInfoReviewPoint.vue";
import LoadingPage from "./commons/LoadingPage.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import AnswerResult from "../components/review/AnswerResult.vue";
import QuizQuestion from "../components/review/QuizQuestion.vue";
import RadioButtons from "../components/form/RadioButtons.vue";
import ShowReviewPoint from "../components/review/ShowReviewPoint.vue";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    answerId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    LoadingPage,
    AnswerResult,
    QuizQuestion,
    RadioButtons,
    ShowReviewPoint,
    NoteInfoReviewPoint,
  },
  data() {
    return {
      answeredQuestion: undefined as Generated.AnsweredQuestion | undefined,
      showQuiz: "review point",
    };
  },
  computed: {
    reviewPoint() {
      return this.answeredQuestion?.reviewPoint;
    },
  },
  methods: {
    onSelfEvaluated(reviewPoint: Generated.ReviewPoint) {
      if (!this.answeredQuestion) return;
      this.answeredQuestion = {
        ...this.answeredQuestion,
        reviewPoint,
      };
    },
    async fetchData() {
      this.answeredQuestion = await this.api.reviewMethods.getAnswer(
        this.answerId,
      );
    },
  },
  watch: {
    answerId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>

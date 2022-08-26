<template>
  <div class="container">
    <LoadingPage v-bind="{ loading, contentExists: !!answerResult }">
      <AnswerResult v-if="answerResult" v-bind="{ answerResult }" />

      <RadioButtons
        v-model="showQuiz"
        :options="[
          { value: 'review point', label: 'Review Point' },
          { value: 'quiz', label: 'Quiz' },
        ]"
      />
      <QuizQuestion
        v-if="showQuiz === 'quiz'"
        v-bind="{ quizQuestion: answerResult?.quizQuestion, historyWriter }"
      />
      <div v-else-if="reviewPoint">
        <ShowReviewPoint v-bind="{ reviewPoint, historyWriter }" />
        <NoteInfoReviewPoint
          v-bind="{ reviewPoint }"
          @self-evaluated="onSelfEvaluated($event)"
        />
      </div>
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
import { HistoryWriter } from "../store/history";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true });
  },
  props: {
    answerId: { type: Number, required: true },
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
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
      answerResult: undefined as Generated.AnswerViewedByUser | undefined,
      showQuiz: "review point",
    };
  },
  computed: {
    reviewPoint() {
      return this.answerResult?.reviewPoint;
    },
  },
  methods: {
    onSelfEvaluated(reviewPoint: Generated.ReviewPoint) {
      if (!this.answerResult) return;
      this.answerResult = {
        ...this.answerResult,
        reviewPoint,
      };
    },
    async fetchData() {
      this.answerResult = await this.api.reviewMethods.getAnswer(this.answerId);
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

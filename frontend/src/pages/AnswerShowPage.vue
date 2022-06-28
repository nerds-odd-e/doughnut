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
        :quiz-question="answerResult.quizQuestion"
      />
      <div v-else>
        <ReviewPointAsync
          v-if="reviewPoint"
          v-bind="{ reviewPointId: reviewPoint.id }"
        />
      </div>
    </LoadingPage>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import LoadingPage from "./commons/LoadingPage.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import AnswerResult from "../components/review/AnswerResult.vue";
import QuizQuestion from "../components/review/QuizQuestion.vue";
import RadioButtons from "../components/form/RadioButtons.vue";
import ReviewPointAsync from "../components/review/ReviewPointAsync.vue";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true });
  },
  props: { answerId: { type: Number, required: true } },
  components: {
    LoadingPage,
    AnswerResult,
    QuizQuestion,
    RadioButtons,
    ReviewPointAsync,
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

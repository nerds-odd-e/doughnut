<template>
  <div class="container">
    <LoadingPage v-bind="{ loading, contentExists: !!answerResult }">
      <AnswerResult v-if="answerResult" v-bind="{ answerResult }" />

      <RadioButtons
        v-model="showQuiz"
        :options="[
          { value: false, label: 'Review Point' },
          { value: true, label: 'Quiz' },
        ]"
      />
      <QuizQuestion v-if="showQuiz" :quizQuestion="answerResult.quizQuestion" />
      <div v-else>
        <ReviewPointAsync v-bind="{ reviewPointId: reviewPointViewedByUser?.reviewPoint.id }" />

      </div>
    </LoadingPage>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import LoadingPage from "./commons/LoadingPage.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import AnswerResult from "../components/review/AnswerResult.vue";
import QuizQuestion from "../components/review/QuizQuestion.vue";
import RadioButtons from "../components/form/RadioButtons.vue";
import ReviewPointAsync from "../components/review/ReviewPointAsync.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true });
  },
  name: "NoteShowPage",
  props: { answerId: Number },
  components: {
    LoadingPage,
    AnswerResult,
    QuizQuestion,
    RadioButtons,
    ReviewPointAsync
},
  data() {
    return {
      answerResult: undefined as Generated.AnswerViewedByUser | undefined,
      showQuiz: false,
    };
  },
  computed: {
    reviewPointViewedByUser() {
      return this.answerResult?.reviewPoint;
    },
  },
  methods: {
    async fetchData() {
      this.answerResult = await this.storedApi.reviewMethods.getAnswer(this.answerId);
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

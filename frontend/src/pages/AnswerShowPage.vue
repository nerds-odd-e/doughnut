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
        <ShowReviewPoint v-bind="{ reviewPointViewedByUser }" />

        <div class="btn-toolbar justify-content-between">
          <label v-if="nextReviewAt" v-text="nextReviewAt" />
          <template v-else>
            <SelfEvaluateButtons @selfEvaluate="selfEvaluate" />
            <button
              class="btn"
              title="remove this note from review"
              @click="$emit('removeFromReview')"
            >
              <SvgNoReview />
            </button>
          </template>
        </div>
      </div>
    </LoadingPage>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import LoadingPage from "./commons/LoadingPage.vue";
import NoteSphereComponent from "../components/notes/views/NoteSphereComponent.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import AnswerResult from "../components/review/AnswerResult.vue";
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import ShowReviewPoint from "../components/review/ShowReviewPoint.vue";
import SelfEvaluateButtons from "../components/review/SelfEvaluateButtons.vue";
import SvgNoReview from "../components/svgs/SvgNoReview.vue";
import QuizQuestion from "../components/review/QuizQuestion.vue";
import RadioButtons from "../components/form/RadioButtons.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true });
  },
  name: "NoteShowPage",
  props: { answerId: Number },
  components: {
    LoadingPage,
    NoteSphereComponent,
    AnswerResult,
    NoteStatisticsButton,
    ShowReviewPoint,
    SelfEvaluateButtons,
    SvgNoReview,
    QuizQuestion,
    RadioButtons,
  },
  data() {
    return {
      answerResult: undefined as Generated.AnswerViewedByUser | undefined,
      nextReviewAt: undefined as string | undefined,
      showQuiz: false,
    };
  },
  computed: {
    reviewPointViewedByUser() {
      return this.answerResult?.reviewPoint;
    },
    reviewPoint() {
      return this.reviewPointViewedByUser?.reviewPoint;
    },
    noteId() {
      return this.reviewPoint?.noteId;
    },
    linkId() {
      return this.reviewPoint?.linkId;
    },
  },
  methods: {
    async fetchData() {
      this.answerResult = await this.storedApi.reviewMethods.getAnswer(this.answerId);
    },
    selfEvaluate(data: string) {
      this.storedApi.reviewMethods
        .selfEvaluate(this.reviewPoint.id, {
          selfEvaluation: data,
        })
        .then((res) => {
          this.nextReviewAt = res.nextReviewAt;
        });
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

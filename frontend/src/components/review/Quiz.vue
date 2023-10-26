<template>
  <div v-if="!minimized" class="content">
    <LoadingPage :content-exists="currentQuestionFetched">
      <template v-if="currentQuestionFetched">
        <div v-if="!currentQuizQuestion">
          <JustReview
            v-bind="{
              reviewPointId: currentReviewPointId,
              storageAccessor,
            }"
            @reviewed="onAnswered($event)"
          />
        </div>
        <QuizQuestion
          v-else
          v-bind="{
            quizQuestion: currentQuizQuestion,
          }"
          @answered="onAnswered($event)"
          :key="currentQuizQuestion.quizQuestionId"
        />
      </template>
    </LoadingPage>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import _ from "lodash";
import QuizQuestion from "./QuizQuestion.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import { StorageAccessor } from "../../store/createNoteStorage";
import JustReview from "./JustReview.vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    minimized: Boolean,
    reviewPoints: {
      type: Object as PropType<number[]>,
      required: true,
    },
    currentIndex: {
      type: Number,
      required: true,
    },
    eagerFetchCount: {
      type: Number,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["answered"],
  components: {
    QuizQuestion,
    JustReview,
    LoadingPage,
  },
  data() {
    return {
      quizQuestionCache: [] as (Generated.QuizQuestion | undefined)[],
      eagerFetchUntil: 0,
      fetching: false,
    };
  },
  computed: {
    currentReviewPointId() {
      return this.reviewPointIdAt(this.currentIndex);
    },
    currentQuestionFetched() {
      return this.quizQuestionCache.length > this.currentIndex;
    },
    currentQuizQuestion() {
      return this.quizQuestionCache[this.currentIndex];
    },
  },
  watch: {
    minimized() {
      this.selectPosition();
    },
    QuizQuestions() {
      this.quizQuestionCache = [];
      this.eagerFetchUntil = 0;
      this.fetchQuestion();
    },
    currentIndex() {
      this.fetchQuestion();
    },
    currentQuizQuestion() {
      this.selectPosition();
    },
  },
  methods: {
    reviewPointIdAt(index: number): number | undefined {
      if (this.reviewPoints && index < this.reviewPoints.length) {
        return this.reviewPoints[index] as number;
      }
      return undefined;
    },

    selectPosition() {
      if (this.minimized) return;
    },

    async fetchQuestion() {
      this.eagerFetchUntil = _.max([
        this.eagerFetchUntil,
        this.currentIndex + this.eagerFetchCount,
      ]) as number;
      if (!this.fetching) {
        this.fetching = true;
        await this.fetchNextQuestion();
        this.fetching = false;
      }
    },

    async fetchNextQuestion() {
      const index = this.quizQuestionCache.length;
      if (this.eagerFetchUntil <= index) return;
      const reviewPointId = this.reviewPointIdAt(index);
      if (reviewPointId === undefined) return;
      try {
        const question =
          await this.silentApi.reviewMethods.getRandomQuestionForReviewPoint(
            reviewPointId,
          );
        this.quizQuestionCache.push(question);
      } catch (e) {
        this.quizQuestionCache.push(undefined);
      }
      await this.fetchNextQuestion();
    },

    onAnswered(answerResult: Generated.AnsweredQuestion) {
      this.$emit("answered", answerResult);
    },
  },
  async mounted() {
    this.fetchQuestion();
  },
});
</script>

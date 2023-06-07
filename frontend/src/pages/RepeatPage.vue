<template>
  <div class="header" :class="currentResult ? 'repeat-paused' : ''">
    <RepeatProgressBar
      v-bind="{
        finished,
        toRepeatCount,
        previousResultCursor,
      }"
      @view-last-result="viewLastResult($event)"
    >
    </RepeatProgressBar>
  </div>
  <template v-if="!minimized">
    <div class="content">
      <div class="inner-box">
        <template v-if="currentQuizQuestion">
          <QuizQuestion
            v-bind="{
              quizQuestion: currentQuizQuestion,
              storageAccessor,
            }"
            @answered="onAnswered($event)"
            @reload-needed="fetchDueReviewPoints"
            :key="currentQuizQuestion.quizQuestion.reviewPoint"
          />
        </template>
        <template v-else-if="finished > 0">
          <div class="alert alert-success">
            You have finished all repetitions for this half a day!
          </div>
          <ReviewHome />
        </template>
      </div>
    </div>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import _ from "lodash";
import QuizQuestion from "../components/review/QuizQuestion.vue";
import ReviewHome from "./ReviewHome.vue";
import RepeatProgressBar from "../components/review/RepeatProgressBar.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  name: "RepeatPage",
  props: {
    minimized: Boolean,
    max: Number,
    dueindays: Number,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    QuizQuestion,
    RepeatProgressBar,
    ReviewHome,
  },
  data() {
    return {
      repetition: undefined as Generated.DueReviewPoints | undefined,
      currentQuizQuestion: undefined as
        | Generated.QuizQuestionViewedByUser
        | undefined,
      previousResults: [] as Generated.AnswerResult[],
      previousResultCursor: undefined as number | undefined,
    };
  },
  computed: {
    currentResult() {
      if (this.previousResultCursor === undefined) return undefined;
      return this.previousResults[this.previousResultCursor];
    },
    finished() {
      return this.previousResults.length;
    },
    toRepeatCount() {
      return (this.repetition?.toRepeat?.length || 0) - this.finished;
    },
  },
  methods: {
    viewLastResult(cursor: number | undefined) {
      this.previousResultCursor = cursor;
      if (this.currentResult) {
        const { answerId } = this.currentResult;
        this.$router.push({ name: "repeat-answer", params: { answerId } });
        return;
      }
      this.$router.push({ name: "repeat" });
    },

    selectPosition() {
      this.storageAccessor.selectPosition(
        undefined,
        this.currentQuizQuestion?.notebookPosition
      );
    },

    async fetchDueReviewPoints() {
      this.repetition = await this.api.reviewMethods.getDueReviewPoints(
        this.max,
        this.dueindays
      );
      if (this.repetition?.toRepeat?.length === 0) {
        this.repetition = undefined;
        if (this.finished === 0) {
          this.$router.push({ name: "reviews" });
        }
        return;
      }
      if (this.api.testability.getEnvironment() !== "testing") {
        this.repetition.toRepeat = _.shuffle(this.repetition.toRepeat);
      }
      await this.fetchQuestion();
    },

    async fetchQuestion() {
      if (
        !this.repetition ||
        this.finished >= this.repetition.toRepeat.length
      ) {
        this.currentQuizQuestion = undefined;
        return;
      }
      this.currentQuizQuestion =
        await this.api.reviewMethods.getRandomQuestionForReviewPoint(
          this.repetition.toRepeat[this.finished]
        );
      this.selectPosition();
    },

    onAnswered(answerResult: Generated.AnswerResult) {
      this.previousResults.push(answerResult);
      if (!answerResult.correct) {
        this.viewLastResult(this.previousResults.length - 1);
      }
      this.fetchQuestion();
    },
  },
  watch: {
    minimized() {
      if (!this.minimized) {
        this.selectPosition();
      }
    },
    max() {
      this.fetchDueReviewPoints();
    },
  },

  async mounted() {
    this.fetchDueReviewPoints();
  },
});
</script>

<style>
.repeat-paused {
  background-color: rgba(50, 150, 50, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

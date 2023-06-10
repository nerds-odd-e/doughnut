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
            :key="currentQuizQuestion.quizQuestion.reviewPoint"
          />
        </template>
        <template v-else-if="noMoreToRepeat">
          <div class="alert alert-success">
            You have finished all repetitions for this half a day!
          </div>
          <div>
            <button
              role="button"
              class="btn btn-secondary"
              @click="loadMore(3)"
            >
              Load more from next 3 days
            </button>
            <button
              role="button"
              class="btn btn-secondary"
              @click="loadMore(7)"
            >
              Load more from next 7 days
            </button>
            <button
              role="button"
              class="btn btn-secondary"
              @click="loadMore(14)"
            >
              Load more from next 14 days
            </button>
          </div>
        </template>
      </div>
    </div>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import _ from "lodash";
import QuizQuestion from "../components/review/QuizQuestion.vue";
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
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    QuizQuestion,
    RepeatProgressBar,
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
    noMoreToRepeat() {
      return this.toRepeatCount <= 0;
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

    async loadMore(dueInDays?: number) {
      this.repetition = await this.api.reviewMethods.getDueReviewPoints(
        dueInDays
      );
      if (this.repetition?.toRepeat?.length === 0) {
        this.repetition = undefined;
        return;
      }
      if (this.api.testability.getEnvironment() !== "testing") {
        this.repetition.toRepeat = _.shuffle(this.repetition.toRepeat);
      }
      await this.fetchQuestion();
    },

    async fetchQuestion() {
      if (!this.repetition || this.noMoreToRepeat) {
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
  },

  async mounted() {
    this.loadMore(0);
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

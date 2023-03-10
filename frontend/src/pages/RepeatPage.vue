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
        <template v-if="repetition">
          <QuizQuestion
            v-bind="{
              quizQuestion: repetition.quizQuestion,
              storageAccessor,
            }"
            @answered="onAnswered($event)"
            @reload-needed="fetchData"
            :key="repetition.quizQuestion.quizQuestion.reviewPoint"
          />
        </template>
        <template v-else>
          <div v-if="finished > 0" class="alert alert-success">
            You have finished all repetitions for this half a day!
          </div>
        </template>
      </div>
    </div>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
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
      repetition: undefined as Generated.RepetitionForUser | undefined,
      toRepeat: undefined as undefined | number[],
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
      return this.toRepeat?.length || 0;
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
        this.repetition?.quizQuestion.notebookPosition
      );
    },
    async fetchData() {
      try {
        this.repetition = await this.api.reviewMethods.getNextReviewItem();
        this.toRepeat = this.repetition?.toRepeat;
        this.selectPosition();
      } catch (_e) {
        this.repetition = undefined;
        if (this.finished === 0) {
          this.$router.push({ name: "reviews" });
        }
      }
    },

    onAnswered(answerResult: Generated.AnswerResult) {
      this.previousResults.push(answerResult);
      this.toRepeat?.pop();
      if (!answerResult.correct) {
        this.viewLastResult(this.previousResults.length - 1);
      }
      this.fetchData();
    },
  },
  watch: {
    minimized() {
      if (!this.minimized) {
        this.selectPosition();
      }
    },
  },

  mounted() {
    this.fetchData();
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

<template>
  <ContainerPage v-bind="{ loading, contentExists: true }">
    <div :class="currentResult ? 'repeat-paused' : ''">
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
    </template>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import QuizQuestion from "../components/review/QuizQuestion.vue";
import ContainerPage from "./commons/ContainerPage.vue";
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
    ContainerPage,
    RepeatProgressBar,
  },
  data() {
    return {
      repetition: undefined as Generated.RepetitionForUser | undefined,
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
      return this.repetition?.toRepeatCount || 0;
    },
  },
  methods: {
    loadNew(resp?: Generated.RepetitionForUser) {
      this.repetition = resp;
      this.storageAccessor.selectPosition(
        undefined,
        resp?.quizQuestion.notebookPosition
      );
    },

    viewLastResult(cursor: number | undefined) {
      this.previousResultCursor = cursor;
      if (this.currentResult) {
        const { answerId } = this.currentResult;
        this.$router.push({ name: "repeat-answer", params: { answerId } });
        return;
      }
      this.$router.push({ name: "repeat" });
    },

    async fetchData() {
      try {
        this.repetition = await this.api.reviewMethods.getNextReviewItem();
      } catch (_e) {
        this.repetition = undefined;
        if (this.finished === 0) {
          this.$router.push({ name: "reviews" });
        }
      }
    },

    onAnswered(answerResult: Generated.AnswerResult) {
      this.previousResults.push(answerResult);
      this.repetition = answerResult.nextRepetition;
      if (answerResult.correct) {
        return;
      }
      this.viewLastResult(this.previousResults.length - 1);
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

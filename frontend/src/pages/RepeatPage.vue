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
    <template v-if="!nested">
      <QuizQuestion
        v-if="repetition?.quizQuestion"
        v-bind="{
          quizQuestion: repetition?.quizQuestion,
        }"
        @answer="processAnswer($event)"
        :key="reviewPointId"
      />
      <template v-else-if="reviewPointId && repetition?.reviewPoint">
        <ReviewPointAsync
          v-bind="{
            reviewPointId,
          }"
          @self-evaluated="fetchData"
          :key="repetition.reviewPoint"
        />
      </template>
      <template v-else>
        <div class="alert alert-success">
          You have finished all repetitions for this half a day!
        </div>
      </template>
    </template>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import QuizQuestion from "../components/review/QuizQuestion.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import RepeatProgressBar from "../components/review/RepeatProgressBar.vue";
import ReviewPointAsync from "../components/review/ReviewPointAsync.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import usePopups from "../components/commons/Popups/usePopup";

export default defineComponent({
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  name: "RepeatPage",
  props: { nested: Boolean },
  components: {
    QuizQuestion,
    ContainerPage,
    RepeatProgressBar,
    ReviewPointAsync,
  },
  data() {
    return {
      repetition: undefined as Generated.RepetitionForUser | undefined,
      previousResults: [] as Generated.AnswerResult[],
      previousResultCursor: undefined as number | undefined,
      toRepeatCount: 0,
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
    reviewPointId() {
      return this.repetition?.reviewPoint;
    },
  },
  methods: {
    loadNew(resp?: Generated.RepetitionForUser) {
      this.repetition = resp;
      if (resp) {
        this.toRepeatCount = resp.toRepeatCount;
        this.$router.push({ name: "repeat-quiz" });
      }
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

    fetchData() {
      this.api.reviewMethods
        .getNextReviewItem()
        .then(this.loadNew)
        .catch(() => {
          this.repetition = undefined;
        });
    },

    async noLongerExist() {
      await this.popups.alert(
        "This review point doesn't exist any more or is being skipped now. Moving on to the next review point..."
      );
      return this.fetchData();
    },

    processAnswer(answerData: Generated.Answer) {
      this.api.reviewMethods
        .processAnswer(answerData)
        .then((res: Generated.AnswerResult) => {
          this.previousResults.push(res);
          this.loadNew(res.nextRepetition);
          if (res.correct) {
            return;
          }
          this.viewLastResult(this.previousResults.length - 1);
        })
        .catch(() => this.noLongerExist());
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

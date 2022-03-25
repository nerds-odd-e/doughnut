<template>
  <ContainerPage v-bind="{ loading, contentExists: true }">
    <Minimizable :minimized="nested" staticHeight="75px">
      <template #minimizedContent>
        <div class="repeat-container" v-on:click="backToRepeat()">
          <RepeatProgressBar
            v-bind="{
              finished,
              toRepeatCount,
              hasLastResult,
            }"
            @viewLastResult="viewLastResult()"
          >
          </RepeatProgressBar>
        </div>
      </template>
      <template #fullContent>
        <RepeatProgressBar
          v-bind="{
            finished,
            toRepeatCount,
            hasLastResult,
          }"
          @viewLastResult="viewLastResult()"
        />
        <div class="alert alert-success" v-if="latestAnswerCorrrect">Correct!</div>
        <QuizQuestion
          v-if="repetition?.quizQuestion"
          v-bind="{
            quizQuestion: repetition?.quizQuestion,
          }"
          @answer="processAnswer($event)"
          @removeFromReview="removeFromReview"
          :key="reviewPointId"
        />
        <template v-else>
          <div class="alert alert-success">
            You have finished all repetitions for this half a day!
          </div>
        </template>
      </template>
    </Minimizable>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Minimizable from "../components/commons/Minimizable.vue";
import QuizQuestion from "../components/review/QuizQuestion.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import RepeatProgressBar from "../components/review/RepeatProgressBar.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import usePopups from "../components/commons/Popups/usePopup";

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi(), ...usePopups() };
  },
  name: "RepeatPage",
  props: { nested: Boolean },
  components: {
    Minimizable,
    QuizQuestion,
    ContainerPage,
    RepeatProgressBar,
  },
  data() {
    return {
      repetition: undefined as Generated.RepetitionForUser | undefined,
      previousResults: [] as Generated.AnswerResult[],
      previousResultCursor: undefined as number | undefined,
      finished: 0,
      toRepeatCount: 0,
    };
  },
  computed: {
    reviewPointId() {
      return this.repetition?.quizQuestion?.quizQuestion?.reviewPoint;
    },
    hasLastResult() {
      return this.previousResultCursor !== undefined
    },
    latestAnswerCorrrect() {
      return this.previousResults.length > 0 && this.previousResults[this.previousResults.length - 1].correct
    },
  },
  methods: {
    backToRepeat() {
      this.previousResultCursor = this.previousResults.length - 1
      this.$router.push({ name: "repeat" });
    },
    loadNew(resp?: Generated.RepetitionForUser) {
      this.repetition = resp;
      if (resp) {
        this.toRepeatCount = resp.toRepeatCount;
        this.$router.push({ name: "repeat-quiz" });
      }
    },

    viewLastResult() {
      if(this.previousResultCursor === undefined) return;
      const answerId = this.previousResults[this.previousResultCursor].answerId
      if(this.previousResultCursor > 0){
         this.previousResultCursor -= 1
      }
      else {
        this.previousResultCursor = undefined
      }
      this.$router.push({ name: "repeat-answer", params: { answerId } });
    },

    fetchData() {
      this.storedApi.reviewMethods
        .getNextReviewItem()
        .then(this.loadNew)
        .catch((e) => {
          this.$router.push({ name: "reviews" });
        });
    },

    async noLongerExist() {
      await this.popups.alert(
        "This review point doesn't exist any more or is being skipped now. Moving on to the next review point..."
      );
      return this.fetchData();
    },

    processAnswer(answerData: Generated.Answer) {
      this.storedApi.reviewMethods
        .processAnswer(answerData)
        .then((res: Generated.AnswerResult) => {
          this.previousResults.push(res)
          this.previousResultCursor = this.previousResults.length - 1
          if (res.correct) {
            this.finished += 1;
            this.toRepeatCount -= 1;
            this.loadNew(res.nextRepetition);
            return;
          }
          this.$router.push({
            name: "repeat-answer",
            params: { answerId: res.answerId },
          });
        })
        .catch((err) => this.noLongerExist());
    },

    async removeFromReview() {
      if (
        !(await this.popups.confirm(
          `Are you sure to hide this from reviewing in the future?`
        ))
      ) {
        return;
      }
      this.api.reviewMethods
        .removeFromReview(this.reviewPointId)
        .then((r) => this.fetchData());
    },
  },

  mounted() {
    this.fetchData();
  },
});
</script>

<style>
.repeat-container {
  background-color: rgba(50, 150, 50, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

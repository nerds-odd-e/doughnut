<template>
  <ContainerPage v-bind="{ loading, contentExists: true }">
        <div :class="pausing ? 'repeat-paused' : ''">
          <RepeatProgressBar
            v-bind="{
              finished,
              toRepeatCount,
              previousResultCursor,
            }"
            @viewLastResult="viewLastResult($event)"
          >
          </RepeatProgressBar>
        </div>
      <template v-if="!nested">
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
          <template v-if="finished > 0">
            <div class="alert alert-success">
              You have finished all repetitions for this half a day!
            </div>
          </template>
        </template>
      </template>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
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
    QuizQuestion,
    ContainerPage,
    RepeatProgressBar,
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
    pausing() {
      return this.previousResultCursor !== undefined
    },
    finished() {
      return this.previousResults.length
    },
    reviewPointId() {
      return this.repetition?.quizQuestion?.quizQuestion?.reviewPoint;
    },
    latestAnswer() {
      if(this.previousResults.length === 0) return
      return this.previousResults[this.previousResults.length - 1]
    },
    latestAnswerCorrrect() {
      return this.latestAnswer?.correct
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
      this.previousResultCursor = cursor
      if(this.pausing) {
        const answerId = this.previousResults[this.previousResultCursor].answerId;
        this.$router.push({ name: "repeat-answer", params: { answerId } });
        return
      }
      this.$router.push({ name: "repeat" });
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
          this.previousResults.push(res);
          this.loadNew(res.nextRepetition);
          if (res.correct) {
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
.repeat-paused {
  background-color: rgba(50, 150, 50, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

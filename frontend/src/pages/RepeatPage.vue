<template>
  <ContainerPage v-bind="{ loading, contentExists: !!reviewPoint }">
      <Minimizable :minimized="nested" staticHeight="75px">
        <template #minimizedContent>
          <div class="repeat-container" v-on:click="backToRepeat()">
            <RepeatProgressBar
              :allowPause="!quizMode"
              :btn="`play`"
              v-bind="{
                linkId,
                noteId,
                finished,
                toRepeatCount: repetition.toRepeatCount,
                hasLastResult,
              }"
              @viewLastResult="viewLastResult()"
            >
            </RepeatProgressBar>
          </div>
        </template>
        <template #fullContent>
          <RepeatProgressBar
            :allowPause="!quizMode"
            v-bind="{
              linkId,
              noteId,
              finished,
              toRepeatCount: repetition.toRepeatCount,
              hasLastResult,
            }"
            @viewLastResult="viewLastResult()"
          />
          <div class="alert alert-success" v-if="lastAnswerCorrrect">
            Correct!
          </div>
          <QuizQuestion
            v-if="quizMode"
            v-bind="{
              quizQuestion: repetition.quizQuestion,
            }"
            @answer="processAnswer($event)"
            @selfEvaluate="selfEvaluate($event)"
            @removeFromReview="removeFromReview"
            :key="reviewPoint.id"
          />
          <template v-else>
              <Repetition
                v-bind="{ 
                  reviewPointViewedByUser,
                  answerResult
                }"
                @selfEvaluate="selfEvaluate($event)"
                @removeFromReview="removeFromReview"
              />
          </template>
        </template>
      </Minimizable>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Minimizable from "../components/commons/Minimizable.vue";
import QuizQuestion from "../components/review/QuizQuestion.vue";
import Repetition from "../components/review/Repetition.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import RepeatProgressBar from "../components/review/RepeatProgressBar.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import usePopups from "../components/commons/Popups/usePopup";

export default defineComponent({
  setup() {
    return {...useStoredLoadingApi(), ...usePopups()};
  },
  name: "RepeatPage",
  props: { nested: Boolean },
  components: {
    Minimizable,
    QuizQuestion,
    Repetition,
    ContainerPage,
    RepeatProgressBar,
  },
  data() {
    return {
      repetition: undefined as Generated.RepetitionForUser | undefined,
      answerResult: undefined as Generated.AnswerResult | undefined,
      lastResult: undefined,
      finished: 0,
    };
  },
  computed: {
    reviewPointViewedByUser() {
      return this.repetition?.reviewPointViewedByUser;
    },
    reviewPoint() {
      return this.reviewPointViewedByUser?.reviewPoint;
    },
    quizMode() {
      return !this.answerResult;
    },
    linkId() {
      return this.reviewPoint?.linkId
    },
    noteId() {
      return this.reviewPoint?.noteId
    },
    reviewPointId() {
      return this.reviewPoint?.id
    },
    hasLastResult() {
      return this.lastResult?.answerResult;
    },
    lastAnswerCorrrect() {
      return this.answerResult?.correct;
    }
  },
  methods: {
    backToRepeat() {
      this.$router.push({ name: "repeat" });
    },
    loadNew(resp: Generated.RepetitionForUser) {
      this.lastResult = {
        answerResult: this.answerResult,
        repetition: this.repetition,
      };

      this.repetition = resp;
      this.answerResult = null;
      if (!this.reviewPoint) {
        this.$router.push({ name: "reviews" });
        return;
      }
      if (this.repetition?.quizQuestion) {
        this.$router.push({ name: "repeat-quiz" });
        return;
      }
      this.resetRoute();
    },

    viewLastResult() {
      const last = this.lastResult.answerResult;
      this.lastResult = null;
      this.$router.push({ name: "repeat-answer", params: {answerId: last.answerId} });
    },

    resetRoute() {
      this.$router.push({ name: "repeat", replace: true });
    },

    fetchData() {
      this.storedApi.reviewMethods.getNextReviewItem().then(
        this.loadNew
      );
    },

    async noLongerExist() {
      await this.popups.alert(
        "This review point doesn't exist any more or is being skipped now. Moving on to the next review point..."
      );
      return this.fetchData()
    },

    processAnswer(answerData: Generated.Answer) {
      this.storedApi.reviewMethods.processAnswer(answerData)
      .then((res: Generated.AnswerResult) => {
        this.answerResult = res
        if (res.correct) {
          this.finished += 1
          this.repetition.toRepeatCount -= 1
          if (res.nextRepetition?.reviewPointViewedByUser) {
            this.loadNew(res.nextRepetition)
          }
          this.resetRoute()
          return
        }
        this.$router.push({ name: "repeat-answer", params: {answerId: res.answerId} });
      })
    },

    selfEvaluate(data) {
      if (data !== "again" && !this.answerResult) {
        this.finished += 1
        this.repetition.toRepeatCount -= 1
      }

      this.storedApi.reviewMethods.selfEvaluate(this.reviewPointId,
        { selfEvaluation: data, increaseRepeatCount: !this.answerResult },
      )
      .then(this.loadNew)
      .catch((err) => this.noLongerExist())
    },
    async removeFromReview() {
      if (!(await this.popups.confirm(
          `Are you sure to hide this from reviewing in the future?`
        ))
      ) {
        return;
      }
      this.api.reviewMethods.removeFromReview(this.reviewPointId)
      .then((r) => this.fetchData())
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

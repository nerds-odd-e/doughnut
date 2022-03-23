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
              <Repetition
                v-bind="{
                  reviewPointViewedByUser,
                  answerResult,
                  compact: true,
                }"
                @selfEvaluate="selfEvaluate($event)"
                @removeFromReview="removeFromReview"
              />
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
          <Quiz
            v-if="quizMode"
            v-bind="{
              reviewPointViewedByUser: repetition.reviewPointViewedByUser,
              quizQuestion: repetition.quizQuestion,
            }"
            @answer="processAnswer($event)"
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
              <NoteStatisticsButton v-if="noteId" :noteId="noteId" />
              <NoteStatisticsButton v-else :link="linkId" />
          </template>
        </template>
      </Minimizable>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Minimizable from "../components/commons/Minimizable.vue";
import Quiz from "../components/review/Quiz.vue";
import Repetition from "../components/review/Repetition.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
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
    Quiz,
    Repetition,
    ContainerPage,
    NoteStatisticsButton,
    RepeatProgressBar,
  },
  data() {
    return {
      repetition: undefined as Generated.RepetitionForUser | undefined,
      answerResult: undefined as Generated.AnswerResult | undefined,
      lastResult: null,
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
      return this.repetition?.quizQuestion && !this.answerResult;
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
      this.answerResult = this.lastResult.answerResult;
      this.repetition = this.lastResult.repetition;
      this.lastResult = null;
      this.resetRoute();
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
      this.api.reviewMethods.processAnswer(answerData)
      .then((res: Generated.AnswerResult) => {
        this.answerResult = res
        if (res.correct) {
          this.finished += 1
          this.repetition.toRepeatCount -= 1
          if (res.nextRepetition?.reviewPointViewedByUser) {
            this.loadNew(res.nextRepetition)
          }
        }
        this.resetRoute()
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

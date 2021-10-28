<template>
  <ContainerPage v-bind="{ loading, contentExists: !!repetition }">
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
                  ...reviewPointViewedByUser,
                  answerResult,
                  compact: true,
                }"
                @selfEvaluate="selfEvaluate($event)"
                @reviewPointRemoved="fetchData()"
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
            v-bind="repetition"
            @answer="processAnswer($event)"
          />
          <template v-else>
            <template v-if="reviewPointViewedByUser">
              <Repetition
                v-bind="{ ...reviewPointViewedByUser, answerResult }"
                @selfEvaluate="selfEvaluate($event)"
                @reviewPointRemoved="fetchData()"
              />
              <NoteStatisticsButton v-if="noteId" :noteId="noteId" />
              <NoteStatisticsButton v-else :link="linkId" />
            </template>
          </template>
        </template>
      </Minimizable>
  </ContainerPage>
</template>

<script>
import Minimizable from "../components/commons/Minimizable.vue";
import Quiz from "../components/review/Quiz.vue";
import Repetition from "../components/review/Repetition.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import RepeatProgressBar from "../components/review/RepeatProgressBar.vue";
import { apiSelfEvaluate, apiProcessAnswer, apiGetNextReviewItem } from '../storedApi';

export default {
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
      repetition: null,
      answerResult: null,
      lastResult: null,
      loading: false,
      finished: 0,
    };
  },
  computed: {
    reviewPointViewedByUser() {
      return this.repetition.reviewPointViewedByUser;
    },
    quizMode() {
      return !!this.repetition.quizQuestion && !this.answerResult;
    },
    linkId() {
      if (
        this.reviewPointViewedByUser &&
        this.reviewPointViewedByUser.linkViewedByUser
      )
        return this.reviewPointViewedByUser.linkViewedByUser.id;
    },
    noteId() {
      if (
        this.reviewPointViewedByUser &&
        this.reviewPointViewedByUser.noteWithPosition
      )
        return this.reviewPointViewedByUser.noteWithPosition.note.id;
    },
    reviewPointId() {
      return this.reviewPointViewedByUser.reviewPoint.id
    },
    hasLastResult() {
      return this.lastResult && !!this.lastResult.answerResult;
    },
  },
  methods: {
    backToRepeat() {
      this.$router.push({ name: "repeat" });
    },
    loadNew(resp) {
      this.lastResult = {
        answerResult: this.answerResult,
        repetition: this.repetition,
      };

      this.repetition = resp;
      this.answerResult = null;
      if (!this.repetition.reviewPoint.noteId) {
        this.$router.push({ name: "reviews" });
        return;
      }
      if (!!this.repetition.quizQuestion) {
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
      this.loading = true
      apiGetNextReviewItem().then(
        this.loadNew
      ).finally(() => this.loading = false);
    },

    async noLongerExist() {
      await this.$popups.alert(
        "This review point doesn't exist any more or is being skipped now. Moving on to the next review point..."
      );
      return this.fetchData()
    },

    processAnswer(answerData) {
      this.loading = true
      apiProcessAnswer(this.reviewPointId, answerData)
      .then((res) => {
        this.answerResult = res
        if (res.correct) {
          this.finished += 1
          this.repetition.toRepeatCount -= 1
          if (this.repetition.toRepeatCount > 0) {
            this.fetchData()
          }
        }
        this.resetRoute()
      })
      .finally(() => this.loading = false)
    },

    selfEvaluate(data) {
      if (data !== "again" && !this.answerResult) {
        this.finished += 1
        this.repetition.toRepeatCount -= 1
      }
      this.loading = true

      apiSelfEvaluate(
        this.reviewPointId,
        { selfEvaluation: data, increaseRepeatCount: !this.answerResult },
      )
      .then(this.loadNew)
      .catch((err) => this.noLongerExist())
      .finally(() => this.loading = false)
    },
  },

  mounted() {
    this.fetchData();
  },
};
</script>

<style>
.repeat-container {
  background-color: rgba(50, 150, 50, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

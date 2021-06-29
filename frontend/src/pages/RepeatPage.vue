<template>
  <LoadingPage v-bind="{loading, contentExists: !!repetition}">
    <template v-if="!!repetition" v-bind="{repetition}">
      <Quiz v-if="!!repetition.quizQuestion && !answerResult" v-bind="repetition" @answer="processAnswer($event)"/>
      <template v-else>
        <template v-if="reviewPointViewedByUser">
          <Repetition v-bind="{...reviewPointViewedByUser, answerResult, sadOnly: false}" @selfEvaluate="selfEvaluate($event)"/>
          <NoteStatisticsButton v-if="reviewPointViewedByUser.noteViewedByUser" :noteid="reviewPointViewedByUser.noteViewedByUser.note.id"/>
          <NoteStatisticsButton v-else :link="reviewPointViewedByUser.linkViewedByUser.id"/>
        </template>
      </template>
    </template>
  </LoadingPage>
</template>

<script>
import Quiz from '../components/review/Quiz.vue'
import Repetition from '../components/review/Repetition.vue'
import LoadingPage from "./LoadingPage.vue"
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import { restGet, restPost } from "../restful/restful"
import { relativeRoutePush } from "../routes/relative_routes"

export default {
  name: 'RepeatPage',
  components: { Quiz, Repetition, LoadingPage, NoteStatisticsButton },
  data() {
    return {
      repetition: null,
      answerResult: null,
      loading: false,
    }
  },
  computed: {
    reviewPointViewedByUser(){ return this.repetition.reviewPointViewedByUser }
  },
  methods: {
    loadNew(resp) {
      this.repetition = resp;
      this.answerResult = null;
      if (!this.repetition.reviewPointViewedByUser) {
        relativeRoutePush(this, {name: "reviews"})
        return
      }
      if (!!this.repetition.quizQuestion) {
        relativeRoutePush(this, {name: "quiz"})
      }
    },

    fetchData() {
      restGet(`/api/reviews/repeat`, (r)=>this.loading=r, this.loadNew)
    },

    processAnswer(answerData) {
      restPost(
        `/api/reviews/${this.reviewPointViewedByUser.reviewPoint.id}/answer`,
        answerData,
        r=>this.loading = r,
        res=>this.answerResult = res)
    },

    selfEvaluate(data) {
      restPost(
        `/api/reviews/${this.reviewPointViewedByUser.reviewPoint.id}/self-evaluate`,
        data,
        r=>this.loading=r,
        this.loadNew)
    }
  },
  mounted() {
    this.fetchData()
  }
}
</script>

<template>
  <LoadingPage v-bind="{loading, contentExists: !!repetition}">
    <template v-if="!!repetition">
      <ProgressBar :allowPause="!quizMode" v-bind="{linkId, noteId, finished, toRepeatCount: repetition.toRepeatCount}"/>
      <Quiz v-if="quizMode" v-bind="repetition" @answer="processAnswer($event)"/>
      <template v-else>
        <template v-if="reviewPointViewedByUser">
          <Repetition
            v-bind="{...reviewPointViewedByUser, answerResult, compact: nested}"
            @selfEvaluate="selfEvaluate($event)"
            @updated="refresh()"
            />
          <NoteStatisticsButton v-if="noteId" :noteId="noteId"/>
          <NoteStatisticsButton v-else :link="linkId"/>
        </template>
      </template>
    </template>
  </LoadingPage>
</template>

<script>
import Quiz from '../components/review/Quiz.vue'
import Repetition from '../components/review/Repetition.vue'
import LoadingPage from "./commons/LoadingPage.vue"
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import ProgressBar from "../components/review/ProgressBar.vue"
import { restGet, restPost } from "../restful/restful"

export default {
  name: 'RepeatPage',
  props: { nested: Boolean },
  components: { Quiz, Repetition, LoadingPage, NoteStatisticsButton, ProgressBar },
  data() {
    return {
      repetition: null,
      answerResult: null,
      loading: false,
      finished: 0
    }
  },
  computed: {
    reviewPointViewedByUser(){ return this.repetition.reviewPointViewedByUser },
    quizMode() {return !!this.repetition.quizQuestion && !this.answerResult},
    linkId() {if(this.reviewPointViewedByUser.linkViewedByUser) return this.reviewPointViewedByUser.linkViewedByUser.id},
    noteId() {if(this.reviewPointViewedByUser.noteViewedByUser) return this.reviewPointViewedByUser.noteViewedByUser.note.id},
  },
  methods: {
    loadNew(resp) {
      this.repetition = resp;
      this.answerResult = null;
      if (!this.repetition.reviewPointViewedByUser) {
        this.$router.push({name: "reviews"})
        return
      }
      if (!!this.repetition.quizQuestion) {
        this.$router.push({name: "repeat-quiz"})
        return;
      }
      this.resetRoute()
    },

    resetRoute() {
      this.$router.push({name: "repeat", replace: true})
    },

    fetchData() {
      restGet(`/api/reviews/repeat`, (r)=>this.loading=r).then(this.loadNew)
    },

    async noLongerExist() {
      await this.$popups.alert("This review point doesn't exist any more or is being skipped now. Moving on to the next review point...")
      return this.fetchData()
    },

    refresh() {
      restGet(`/api/review-points/${this.reviewPointViewedByUser.reviewPoint.id}`, r=>this.loading=r)
        .then(res => {
          if(!res || !!res.reviewPoint.removedFromReview) { this.noLongerExist() }
          this.reviewPointViewedByUser = res
        })
        .catch(err => {
          if (err.statusCode === 404) { this.noLongerExist() }
        })
    },

    processAnswer(answerData) {
      restPost(
        `/api/reviews/${this.reviewPointViewedByUser.reviewPoint.id}/answer`,
        answerData,
        r=>this.loading = r)
        .then(res=>{ this.answerResult = res; this.resetRoute() })
    },

    selfEvaluate(data) {
      if (data !== 'again') this.finished += 1
      restPost(
        `/api/reviews/${this.reviewPointViewedByUser.reviewPoint.id}/self-evaluate`,
        data,
        r=>this.loading=r)
        .then(this.loadNew)
    }
  },
  mounted() {
    this.fetchData()
  },
}
</script>

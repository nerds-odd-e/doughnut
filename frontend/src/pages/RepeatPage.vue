<template>
  <LoadingPage v-bind="{loading, contentExists: !!repetition}">
    <template v-if="!!repetition">
          <Quiz v-if="!!repetition.quizQuestion && !answerResult" v-bind="repetition" @answer="processAnswer($event)"/>
          <template v-else>
            <template v-if="reviewPointViewedByUser">
              <Repetition
                v-bind="{...reviewPointViewedByUser, answerResult, compact: nested}"
                @selfEvaluate="selfEvaluate($event)"
                @updated="refresh()"
                />
              <NoteStatisticsButton v-if="reviewPointViewedByUser.noteViewedByUser" :noteId="reviewPointViewedByUser.noteViewedByUser.note.id"/>
              <NoteStatisticsButton v-else :link="reviewPointViewedByUser.linkViewedByUser.id"/>
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
import { restGet, restPost } from "../restful/restful"

export default {
  name: 'RepeatPage',
  props: { nested: Boolean },
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

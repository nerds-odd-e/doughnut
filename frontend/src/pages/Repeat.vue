<template>
  <LoadingThinBar v-if="loading"/>
  <template v-if="!!repetition" v-bind="{repetition}">
    <Quiz v-if="!!repetition.quizQuestion && !answerResult" v-bind="repetition" @answered="answerResult=$event"/>
    <Repetition v-else v-bind="{...repetition.reviewPointViewedByUser, answerResult, sadOnly: false}" @evaluated="loadNew($event)"/>
  </template>
  <div v-else><ContentLoader /></div>
</template>

<script>
import Quiz from '../components/review/Quiz.vue'
import Repetition from '../components/review/Repetition.vue'
import ContentLoader from "../components/ContentLoader.vue"
import LoadingThinBar from "../components/LoadingThinBar.vue"
import {restGet} from "../restful/restful"
import { ref, inject } from 'vue'

export default {
  components: {
    Quiz, Repetition, ContentLoader, LoadingThinBar
  },
  data() {
    return {
      repetition: ref(null),
      answerResult: ref(null),
      loading: ref(false)
    }
  },

  mounted() {
    this.fetchData();
  },

  methods: {
    fetchData() {
      restGet(`/api/reviews/repeat`, (val)=>this.loading=val, this.loadNew)
    },
    loadNew(resp) {
      this.repetition = resp;
      this.answerResult = null;
      if (!this.repetition.reviewPointViewedByUser) {
        this.$router.push({name: "reviews"})
      }
    }
  }

}

</script>

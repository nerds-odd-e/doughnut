<template>
  <LoadingThinBar v-if="loading"/>
  <template v-if="!!repetition" v-bind="{repetition}">
    <Quiz v-if="!!repetition.quizQuestion && !answerResult" v-bind="repetition" @answer="processAnswer($event)"/>
    <template v-else>
    <Repetition v-if="repetition.reviewPointViewedByUser" v-bind="{...repetition.reviewPointViewedByUser, answerResult, sadOnly: false}" @selfEvaluate="selfEvaluate($event)"/>
    </template>
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

    processAnswer(answerData) {
      this.loading = true
      fetch(`/api/reviews/${this.repetition.reviewPointViewedByUser.reviewPoint.id}/answer`, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(answerData)
      })
        .then(res => {
          return res.json();
        })
        .then(resp => {
          this.answerResult = resp
          this.loading = false
        })
        .catch(error => {
          window.alert(error);
        });
    },

    selfEvaluate(data) {
      this.loading = true
      fetch(`/api/reviews/${this.repetition.reviewPointViewedByUser.reviewPoint.id}/self-evaluate`, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
      })
        .then(res => {
          return res.json();
        })
        .then(resp => {
          this.loadNew(resp)
          this.loading = false
        })
        .catch(error => {
          window.alert(error);
        });
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

<template>
  <LoadingPage v-bind="{loading, contentExists: !!repetition}">
    <template v-if="!!repetition" v-bind="{repetition}">
      <Quiz v-if="!!repetition.quizQuestion && !answerResult" v-bind="repetition" @answer="processAnswer($event)"/>
      <template v-else>
      <Repetition v-if="repetition.reviewPointViewedByUser" v-bind="{...repetition.reviewPointViewedByUser, answerResult, sadOnly: false}" @selfEvaluate="selfEvaluate($event)"/>
      </template>
    </template>
  </LoadingPage>
</template>

<script setup>
import Quiz from '../components/review/Quiz.vue'
import Repetition from '../components/review/Repetition.vue'
import LoadingPage from "./LoadingPage.vue"
import { restGet, restPost } from "../restful/restful"
import { ref, inject } from 'vue'

const emit = defineEmit(['redirect'])

const repetition = ref(null)
const answerResult = ref(null)
const loading = ref(false)

const loadNew = (resp) => {
  repetition.value = resp;
  answerResult.value = null;
  if (!repetition.value.reviewPointViewedByUser) {
    emit("redirect", {name: "reviews"})
  }
}

const fetchData = () => {
  restGet(`/api/reviews/repeat`, loading, loadNew)
}

const processAnswer = (answerData) => {
  restPost(
    `/api/reviews/${repetition.value.reviewPointViewedByUser.reviewPoint.id}/answer`,
    answerData,
      loading,
      (res)=>answerResult.value = res)
}

const selfEvaluate = (data) => {
  restPost(
    `/api/reviews/${repetition.value.reviewPointViewedByUser.reviewPoint.id}/self-evaluate`,
    data,
    loading,
    loadNew)
}

fetchData()

</script>

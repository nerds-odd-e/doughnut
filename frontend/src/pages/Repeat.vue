<template>
  <LoadingPage v-bind="{loading, contentExists: !!repetition}">
    <template v-if="!!repetition" v-bind="{repetition}">
      <Quiz v-if="!!repetition.quizQuestion && !answerResult" v-bind="repetition" @answer="processAnswer($event)"/>
      <template v-else>
        <template v-if="reviewPointViewedByUser">
          <Repetition v-bind="{...reviewPointViewedByUser, answerResult, sadOnly: false}" @selfEvaluate="selfEvaluate($event)" @redirect="emit('redirect', $event)"/>
          <NoteStatisticsButton v-if="reviewPointViewedByUser.noteViewedByUser" :noteid="reviewPointViewedByUser.noteViewedByUser.note.id"/>
          <NoteStatisticsButton v-else :link="reviewPointViewedByUser.linkViewedByUser.id"/>
        </template>
      </template>
    </template>
  </LoadingPage>
</template>

<script setup>
import Quiz from '../components/review/Quiz.vue'
import Repetition from '../components/review/Repetition.vue'
import LoadingPage from "./LoadingPage.vue"
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import { restGet, restPost } from "../restful/restful"
import { ref, inject, computed } from 'vue'

const emit = defineEmit(['redirect'])

const repetition = ref(null)
const answerResult = ref(null)
const loading = ref(false)
const reviewPointViewedByUser = computed(()=>repetition.value.reviewPointViewedByUser)
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
    `/api/reviews/${reviewPointViewedByUser.value.reviewPoint.id}/answer`,
    answerData,
      loading,
      (res)=>answerResult.value = res)
}

const selfEvaluate = (data) => {
  restPost(
    `/api/reviews/${reviewPointViewedByUser.value.reviewPoint.id}/self-evaluate`,
    data,
    loading,
    loadNew)
}

fetchData()

</script>

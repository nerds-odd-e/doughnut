<script setup lang="ts">
import { onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { AssessmentAttempt } from "@/generated/backend"

const loaded = ref(false)
const assessmentHistory = ref<AssessmentAttempt[]>([])

const loadAssessmentHistory = () => {
  useLoadingApi()
    .managedApi.restAssessmentController.getAssessmentHistory()
    .then((response) => {
      assessmentHistory.value = response
      loaded.value = true
    })
}

onMounted(() => {
  loadAssessmentHistory()
})
</script>

<template>
  <div class="container">
    <h1>Assessment History</h1>
    <p v-if="loaded && assessmentHistory.length === 0">
      No assessment has been done yet
    </p>
    <table v-else-if="loaded && assessmentHistory.length > 0">
      <thead>
        <tr>
          <th>Notebook</th>
          <th>Correct Answers</th>
          <th>Total Questions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="attempt in assessmentHistory" :key="attempt.id">
          <td>{{ attempt.notebook?.headNote.noteTopic }}</td>
          <td>{{ attempt.answersCorrect }}</td>
          <td>{{ attempt.answersTotal }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped lang="scss">
table {
  width: 100%;
}
</style>

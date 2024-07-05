<script setup lang="ts">
import { onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { AssessmentAttempt } from "@/generated/backend"

const assessmentHistory = ref<AssessmentAttempt[]>([])

const loadAssessmentHistory = () => {
  useLoadingApi()
    .managedApi.restAssessmentController.getAssessmentHistory()
    .then((response) => {
      assessmentHistory.value = response
    })
}

onMounted(() => {
  loadAssessmentHistory()
})
</script>

<template>
  <div class="container">
    <p>No assessment has been done yet</p>
    <h1>Assessment History</h1>
    <table>
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

<style scoped lang="scss"></style>

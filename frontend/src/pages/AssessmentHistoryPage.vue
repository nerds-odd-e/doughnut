<template>
  <ContainerPage
    v-bind="{
      contentLoaded: assessmentHistories !== undefined,
      title: 'My Assessment History',
    }"
  >
    <input
      type="text"
      v-model="filterText"
      placeholder="Filter by notebook name"
      class="daisy-form-control daisy-mb-2"
    />
    <div>
      <table class="assessment-table mt-2">
        <thead>
          <tr>
            <th>Notebook</th>
            <th>Attempt At</th>
            <th>Result</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in filteredAssessmentHistories" :key="row.id">
            <td>{{ row.notebookName }}</td>
            <td>{{ toLocalDateString(row.submittedAt) }}</td>
            <td>{{ row.isPass ? "Pass" : "Fail" }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import type { AssessmentAttempt } from "@generated/doughnut-backend-api"
import { AssessmentController } from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"
import ContainerPage from "./commons/ContainerPage.vue"

const assessmentHistories = ref<undefined | AssessmentAttempt[]>(undefined)
const filterText = ref("")

const toLocalDateString = (date?: string) => {
  if (!date) {
    return ""
  }
  return new Date(date).toLocaleString()
}

const filteredAssessmentHistories = computed(() => {
  if (!assessmentHistories.value) {
    return []
  }
  return assessmentHistories.value.filter((row) =>
    row.notebookName?.toLowerCase().includes(filterText.value.toLowerCase())
  )
})

onMounted(async () => {
  const { data: assessments, error } =
    await AssessmentController.getMyAssessments({})
  if (!error) {
    assessmentHistories.value = assessments!
  }
})
</script>

<style scoped>
.assessment-table {
  border-collapse: collapse;
  width: 100%;
}

.assessment-table th,
.assessment-table td {
  border: 1px solid #dddddd;
  text-align: left;
  padding: 8px;
}

.assessment-table th {
  background-color: #f2f2f2;
}
</style>

<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Welcome To Assessment History',
    }"
  >
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
        <tr
          v-for="(assessmentHistory) in assessmentHistories"
        >
          <td>{{assessmentHistory.notebookTitle}}</td>
          <td>{{assessmentHistory.submittedAt}}</td>
          <td>{{assessmentHistory.answersCorrect}}</td>
        </tr>
      </tbody>
    </table>
  </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { PropType, onMounted, ref } from "vue"
import { AssessmentHistory, User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "./commons/ContainerPage.vue"

const { managedApi } = useLoadingApi()

defineProps({
  user: {
    type: Object as PropType<User>,
    required: false,
  },
})

const assessmentHistories = ref<AssessmentHistory[]>([])

onMounted(async () => {
  assessmentHistories.value =
    await managedApi.restAssessmentController.getAssessmentHistory()
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

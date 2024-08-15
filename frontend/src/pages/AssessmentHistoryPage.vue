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
          <th>ID</th>
          <th>Notebook</th>
          <th>Attempt At</th>
          <th>Result</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(assessmentHistory) in assessmentHistories"
        >
          <td>{{assessmentHistory.id}}</td>
          <td>{{assessmentHistory.notebookTitle}}</td>
          <td>{{toLocalDateString(assessmentHistory.submittedAt)}}</td>
          <td>{{assessmentHistory.result}}</td>
          <td>
            <button
              :disabled="assessmentHistory.result === 'Fail'"
              @click="getCertificate(assessmentHistory.id)"
            >
              Get Certificate
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <Modal
    v-if="certificate != null"
    @close_request="certificate = null"
  >
    <template #body>
      <h2>Congratulations on your Certificate</h2>
      <div>Notebook: {{ certificate.notebook?.headNote.noteTopic.topicConstructor }}</div>
      <div>Expiry: {{ certificate.expiryDate }}</div>
    </template>
  </Modal>
  </ContainerPage>
</template>

<script setup lang="ts">
import { PropType, onMounted, ref } from "vue"
import { AssessmentHistory, Certificate, User } from "@/generated/backend"
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
const certificate = ref<Certificate | null>(null)

const toLocalDateString = (date: string) => {
  return new Date(date).toLocaleString()
}

onMounted(async () => {
  assessmentHistories.value =
    await managedApi.restAssessmentController.getAssessmentHistory()
})

const getCertificate = async (assessmentHistoryId?: number) => {
  if (assessmentHistoryId == null) {
    return
  }

  certificate.value =
    await managedApi.restAssessmentController.getCertificate(
      assessmentHistoryId
    )
}
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

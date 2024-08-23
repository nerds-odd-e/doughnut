<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Welcome To Assessment History',
    }"
  >
  <input
  type="text"
  v-model="filterText"
  placeholder="Filter by notebook title"
  class="form-control mb-2"
/>
<input
  type="checkbox"
  v-model="filterByCertificate"
  class="form-check-input"
/>
<label class="form-check-label">Filter by Certificate</label>
  <div>
    <table class="assessment-table mt-2">
      <thead>
        <tr>
          <th>Notebook</th>
          <th>Attempt At</th>
          <th>Result</th>
          <th>Certificate</th>
          <th>Expirary date</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(assessmentHistory) in filteredAssessmentHistories"
        >
          <td>{{assessmentHistory.notebookTitle}}</td>
          <td>{{toLocalDateString(assessmentHistory.submittedAt)}}</td>
          <td>{{assessmentHistory.isPass ? "Pass" : "Fail"}}</td>
          <td>
            <PopButton
              btn-class="btn btn-light"
              title="View Certificate"
              v-if="assessmentHistory.isPass"
            >
              <CertificatePopup  :assessment-attempt="assessmentHistory" :notebook-id="assessmentHistory.notebookId"></CertificatePopup>
            </PopButton>
          </td>
          <td>{{toLocalDateString(assessmentHistory.certificateExpiresAt)}}</td>
        </tr>
      </tbody>
    </table>
  </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { PropType, computed, onMounted, ref } from "vue"
import { AssessmentAttempt, User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "./commons/ContainerPage.vue"

const { managedApi } = useLoadingApi()

defineProps({
  user: {
    type: Object as PropType<User>,
    required: false,
  },
})

const filterByCertificate = ref(false)
const assessmentHistories = ref<AssessmentAttempt[]>([])
const filterText = ref("")

const toLocalDateString = (date: string) => {
  return new Date(date).toLocaleString()
}

const filteredAssessmentHistories = computed(() => {
  return assessmentHistories.value.filter((assessmentHistory) => {
    const matchesTitle = assessmentHistory.notebookTitle
      ?.toLowerCase()
      .includes(filterText.value.toLowerCase())
    const matchesCertificate =
      !filterByCertificate.value || assessmentHistory.isPass
    return matchesTitle && matchesCertificate
  })
})

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

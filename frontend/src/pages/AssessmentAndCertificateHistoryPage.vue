<template>
  <ContainerPage
    v-bind="{
      contentLoaded: assessmentHistories !== undefined,
      title: 'My Assessment and Certificate History',
    }"
  >
    <input
      type="text"
      v-model="filterText"
      placeholder="Filter by notebook title"
      class="daisy-form-control daisy-mb-2"
    />
    <input type="checkbox" v-model="filterByCertificate" class="daisy-check"
      id="filterByCertificate"
     />
    <label class="daisy-label" for="filterByCertificate">Filter by Certificate</label>
    <div>
      <table class="assessment-table mt-2">
        <thead>
          <tr>
            <th>Notebook</th>
            <th>Attempt At</th>
            <th>Result</th>
            <th>Certificate</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="assessmentAndCertificateHistory in filteredAssessmentHistories">
            <td>{{ assessmentAndCertificateHistory.notebookTitle }}</td>
            <td>{{ toLocalDateString(assessmentAndCertificateHistory.submittedAt) }}</td>
            <td>{{ assessmentAndCertificateHistory.isPass ? "Pass" : "Fail" }}</td>
            <td>
              <PopButton
                btn-class="daisy-btn daisy-btn-light"
                title="View Certificate"
                v-if="assessmentAndCertificateHistory.isPass"
              >
                <CertificatePopup
                  :assessment-attempt="assessmentAndCertificateHistory"
                  :notebook-id="assessmentAndCertificateHistory.notebookId"
                ></CertificatePopup>
              </PopButton>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import type { AssessmentAttempt } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "./commons/ContainerPage.vue"

const { managedApi } = useLoadingApi()

const filterByCertificate = ref(false)
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
  return assessmentHistories.value.filter((assessmentAndCertificateHistory) => {
    const matchesTitle = assessmentAndCertificateHistory.notebookTitle
      ?.toLowerCase()
      .includes(filterText.value.toLowerCase())
    const matchesCertificate =
      !filterByCertificate.value || assessmentAndCertificateHistory.isPass
    return matchesTitle && matchesCertificate
  })
})

onMounted(async () => {
  assessmentHistories.value = await managedApi.services.getMyAssessments()
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

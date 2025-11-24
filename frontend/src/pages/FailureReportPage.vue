<template>
  <ContainerPage
    v-bind="{
      contentLoaded: failureReport !== undefined,
      title: 'Failure Report',
    }"
  >
    <div v-if="!!failureReport">
      <div class="daisy-hero daisy-bg-base-200">
        <h2><p v-text="failureReport.errorName" /></h2>
        <pre v-text="failureReport.errorDetail" />
        <p v-text="failureReport.createDatetime" />
        <a
          class="issue_link"
          :href="githubIssueUrl"
          title="show issue"
          v-text="githubIssueUrl"
        />
      </div>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import { FailureReportController } from "@generated/backend/sdk.gen"
import ContainerPage from "./commons/ContainerPage.vue"
import type { FailureReport } from "@generated/backend"

interface Props {
  failureReportId: number
}

const props = defineProps<Props>()

const failureReport = ref<FailureReport | undefined>(undefined)
const githubIssueUrl = ref<string | undefined>(undefined)

const fetchData = async () => {
  const { data: reportData, error } = await FailureReportController.showFailureReport({
    path: { failureReport: props.failureReportId },
  })
  if (!error) {
    // reportData is guaranteed to be FailureReportForView when error is undefined
    failureReport.value = reportData!.failureReport
    githubIssueUrl.value = reportData!.githubIssueUrl
  }
}

onMounted(() => {
  fetchData()
})
</script>

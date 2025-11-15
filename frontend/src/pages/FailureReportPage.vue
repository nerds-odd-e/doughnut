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
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "./commons/ContainerPage.vue"
import type { FailureReport } from "@generated/backend"

interface Props {
  failureReportId: number
}

const props = defineProps<Props>()
const { managedApi } = useLoadingApi()

const failureReport = ref<FailureReport | undefined>(undefined)
const githubIssueUrl = ref<string | undefined>(undefined)

const fetchData = async () => {
  const res = await managedApi.services.show2({
    failureReport: props.failureReportId,
  })
  failureReport.value = res.failureReport
  githubIssueUrl.value = res.githubIssueUrl
}

onMounted(() => {
  fetchData()
})
</script>

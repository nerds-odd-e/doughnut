<template>
  <ContainerPage
    v-bind="{
      contentLoaded: failureReport !== undefined,
      title: 'Failure Report',
    }"
  >
    <div v-if="!!failureReport" class="daisy-space-y-4">
      <div class="daisy-flex daisy-items-center daisy-gap-2">
        <router-link
          :to="{ name: 'adminDashboard', query: { tab: 'failureReport' } }"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="daisy-h-4 daisy-w-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M10 19l-7-7m0 0l7-7m-7 7h18"
            />
          </svg>
          Back to List
        </router-link>
      </div>

      <div class="daisy-card daisy-bg-base-100 daisy-shadow-lg daisy-border daisy-border-base-300">
        <div class="daisy-card-body">
          <div
            class="daisy-flex daisy-flex-col sm:daisy-flex-row sm:daisy-items-start sm:daisy-justify-between daisy-gap-2"
          >
            <h2
              class="daisy-card-title daisy-text-error daisy-flex daisy-items-center daisy-gap-2"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="daisy-h-6 daisy-w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
              {{ failureReport.errorName }}
            </h2>
            <div class="daisy-badge daisy-badge-ghost">#{{ failureReport.id }}</div>
          </div>

          <div class="daisy-divider daisy-my-2"></div>

          <div
            class="daisy-grid daisy-grid-cols-1 md:daisy-grid-cols-2 daisy-gap-4 daisy-mb-4"
          >
            <div class="daisy-flex daisy-items-center daisy-gap-2 daisy-text-base-content/70">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="daisy-h-5 daisy-w-5"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <span class="daisy-font-medium">Created:</span>
              {{ formatDateTime(failureReport.createDatetime) }}
            </div>
            <div
              v-if="githubIssueUrl"
              class="daisy-flex daisy-items-center daisy-gap-2"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="daisy-h-5 daisy-w-5"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"
                />
              </svg>
              <a
                :href="githubIssueUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="daisy-link daisy-link-primary daisy-flex daisy-items-center daisy-gap-1"
              >
                View GitHub Issue
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="daisy-h-4 daisy-w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"
                  />
                </svg>
              </a>
            </div>
          </div>

          <div class="daisy-collapse daisy-collapse-arrow daisy-bg-base-200">
            <input type="checkbox" checked />
            <div class="daisy-collapse-title daisy-font-medium daisy-flex daisy-items-center daisy-gap-2">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="daisy-h-5 daisy-w-5"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
              Error Details
            </div>
            <div class="daisy-collapse-content">
              <pre
                class="daisy-bg-base-300 daisy-p-4 daisy-rounded-lg daisy-overflow-x-auto daisy-text-sm daisy-whitespace-pre-wrap daisy-break-words"
              >{{ failureReport.errorDetail }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import { FailureReportController } from "@generated/doughnut-backend-api/sdk.gen"
import ContainerPage from "./commons/ContainerPage.vue"
import type { FailureReport } from "@generated/doughnut-backend-api"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

interface Props {
  failureReportId: number
}

const props = defineProps<Props>()

const failureReport = ref<FailureReport | undefined>(undefined)
const githubIssueUrl = ref<string | undefined>(undefined)

const formatDateTime = (dateTime: string) => {
  const date = new Date(dateTime)
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

const fetchData = async () => {
  const { data: reportData, error } = await apiCallWithLoading(() =>
    FailureReportController.showFailureReport({
      path: { failureReport: props.failureReportId },
    })
  )
  if (!error) {
    failureReport.value = reportData!.failureReport
    githubIssueUrl.value = reportData!.githubIssueUrl
  }
}

onMounted(() => {
  fetchData()
})
</script>

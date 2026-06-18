<template>
  <div v-if="status">
    <section class="mb-6">
      <h2 class="text-lg font-semibold mb-2">Manual generation</h2>
      <div class="flex flex-wrap gap-2 items-center">
        <button
          type="button"
          class="daisy-btn daisy-btn-primary"
          data-testid="submit-recent-recall-users-button"
          :disabled="submissionInFlight"
          @click="submitRecentRecallUsers"
        >
          Generate for recent recall users
        </button>
        <button
          type="button"
          class="daisy-btn daisy-btn-primary"
          data-testid="resume-existing-batches-button"
          :disabled="maintenanceInFlight"
          @click="resumeExistingBatches"
        >
          Resume existing batches
        </button>
      </div>
      <div
        v-if="submissionSummaryText"
        class="daisy-alert daisy-alert-success mt-4"
        role="status"
        data-testid="submission-summary"
      >
        {{ submissionSummaryText }}
      </div>
      <div
        v-if="maintenanceSummaryText"
        class="daisy-alert daisy-alert-success mt-4"
        role="status"
        data-testid="maintenance-summary"
      >
        {{ maintenanceSummaryText }}
      </div>
      <div
        v-if="maintenanceFailed"
        class="daisy-alert daisy-alert-error mt-4"
        role="alert"
        data-testid="maintenance-error"
      >
        Resume existing batches failed
      </div>
    </section>

    <section class="mb-6">
      <h2 class="text-lg font-semibold mb-2">Availability</h2>
      <div class="flex flex-wrap gap-2">
        <span
          data-testid="openai-token-badge"
          :class="availabilityBadgeClass(status.openAiTokenConfigured)"
        >
          OpenAI token:
          {{ status.openAiTokenConfigured ? "configured" : "not configured" }}
        </span>
        <span
          data-testid="prod-profile-badge"
          :class="availabilityBadgeClass(status.prodProfileActive)"
        >
          Prod profile:
          {{ status.prodProfileActive ? "active" : "inactive" }}
        </span>
        <span
          data-testid="scheduler-badge"
          :class="availabilityBadgeClass(status.schedulerActive)"
        >
          Hourly maintenance:
          {{ status.schedulerActive ? "registered" : "not registered" }}
        </span>
      </div>
      <div
        class="mt-3 text-sm"
        data-testid="maintenance-run-state"
      >
        Last maintenance:
        {{ lastMaintenanceRunText }}
      </div>
    </section>

    <section class="mb-6">
      <h2 class="text-lg font-semibold mb-2">Batches by status</h2>
      <table class="daisy-table daisy-table-zebra">
        <thead>
          <tr>
            <th>Status</th>
            <th>Count</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="[batchStatus, count] in batchStatusEntries"
            :key="batchStatus"
            data-testid="batch-status-row"
          >
            <td>{{ batchStatus }}</td>
            <td>{{ count }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2 class="text-lg font-semibold mb-2">Request rows by status</h2>
      <table class="daisy-table daisy-table-zebra">
        <thead>
          <tr>
            <th>Status</th>
            <th>Count</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="[requestStatus, count] in requestStatusEntries"
            :key="requestStatus"
            data-testid="request-status-row"
          >
            <td>{{ requestStatus }}</td>
            <td>{{ count }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
  <div v-else>Loading batch question generation status...</div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from "vue"
import { AdminQuestionGenerationBatchController } from "@generated/doughnut-backend-api/sdk.gen"
import type {
  QuestionGenerationBatchAdminStatusDto,
  QuestionGenerationBatchSubmissionSummaryDto,
} from "@generated/doughnut-backend-api"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import {
  formatLastMaintenanceRun,
  formatMaintenanceSummary,
  formatSubmissionSummary,
} from "./questionGenerationBatchStatusText"

const status = ref<QuestionGenerationBatchAdminStatusDto | undefined>(undefined)
const submissionSummary = ref<
  QuestionGenerationBatchSubmissionSummaryDto | undefined
>(undefined)
const maintenanceSummary = ref<
  QuestionGenerationBatchAdminStatusDto | undefined
>(undefined)
const submissionInFlight = ref(false)
const maintenanceInFlight = ref(false)
const maintenanceFailed = ref(false)

const batchStatusEntries = computed(
  () =>
    Object.entries(status.value?.batchCountsByStatus ?? {}) as [
      string,
      number,
    ][]
)

const requestStatusEntries = computed(
  () =>
    Object.entries(status.value?.requestCountsByStatus ?? {}) as [
      string,
      number,
    ][]
)

const fetchStatus = async () => {
  const { data, error } = await apiCallWithLoading(() =>
    AdminQuestionGenerationBatchController.getQuestionGenerationBatchStatus()
  )
  if (!error) {
    status.value = data
  }
}

const submissionSummaryText = computed(() =>
  formatSubmissionSummary(submissionSummary.value)
)

const maintenanceSummaryText = computed(() =>
  formatMaintenanceSummary(maintenanceSummary.value)
)

const lastMaintenanceRunText = computed(() =>
  formatLastMaintenanceRun(status.value)
)

const submitRecentRecallUsers = async () => {
  submissionInFlight.value = true
  try {
    const { data, error } = await apiCallWithLoading(() =>
      AdminQuestionGenerationBatchController.submitRecentRecallUsersForQuestionGenerationBatch()
    )
    if (!error) {
      submissionSummary.value = data
      await fetchStatus()
    }
  } finally {
    submissionInFlight.value = false
  }
}

const resumeExistingBatches = async () => {
  maintenanceInFlight.value = true
  maintenanceFailed.value = false
  try {
    const { data, error } = await apiCallWithLoading(() =>
      AdminQuestionGenerationBatchController.resumeExistingQuestionGenerationBatches()
    )
    if (!error) {
      maintenanceSummary.value = data
      status.value = data
      await fetchStatus()
    } else {
      maintenanceFailed.value = true
    }
  } finally {
    maintenanceInFlight.value = false
  }
}

const availabilityBadgeClass = (available: boolean | undefined): string =>
  available
    ? "daisy-badge daisy-badge-success"
    : "daisy-badge daisy-badge-warning"

onMounted(() => {
  fetchStatus()
})
</script>

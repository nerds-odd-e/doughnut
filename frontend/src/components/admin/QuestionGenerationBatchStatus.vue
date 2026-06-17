<template>
  <div v-if="status">
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
          data-testid="scheduler-badge"
          :class="availabilityBadgeClass(status.schedulerActive)"
        >
          Hourly scheduler:
          {{ status.schedulerActive ? "active (prod)" : "inactive" }}
        </span>
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
import type { QuestionGenerationBatchAdminStatusDto } from "@generated/doughnut-backend-api"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const status = ref<QuestionGenerationBatchAdminStatusDto | undefined>(undefined)

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

const availabilityBadgeClass = (available: boolean | undefined): string =>
  available
    ? "daisy-badge daisy-badge-success"
    : "daisy-badge daisy-badge-warning"

onMounted(() => {
  fetchStatus()
})
</script>

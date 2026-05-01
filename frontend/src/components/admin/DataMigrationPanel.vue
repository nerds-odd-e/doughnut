<template>
  <div aria-live="polite">
    <div class="daisy-alert daisy-alert-info daisy-mb-4" role="status">
      Run migration sends bounded batches to the server; the UI keeps calling until the migration
      reports complete or failed. Progress is persisted so you can refresh the page between batches.
    </div>

    <div v-if="error" class="daisy-alert daisy-alert-error daisy-mb-4" role="alert">
      {{ error }}
    </div>

    <div class="daisy-mb-4" data-testid="data-migration-status">
      <p class="daisy-text-sm daisy-opacity-90">
        {{ summaryLine }}
      </p>
    </div>

    <div
      v-if="hasProgressDetail"
      class="daisy-mb-4 daisy-text-sm daisy-opacity-90"
      data-testid="data-migration-progress"
    >
      <p>
        <span data-testid="data-migration-step">{{ status?.currentStepName ?? "—" }}</span>
        ·
        <span data-testid="data-migration-counts"
          >{{ status?.processedCount ?? 0 }} / {{ status?.totalCount ?? 0 }}</span
        >
        <span v-if="status?.stepStatus"> · {{ status.stepStatus }}</span>
      </p>
      <p v-if="status?.lastError" class="daisy-text-error" data-testid="data-migration-last-error">
        {{ status.lastError }}
      </p>
    </div>

    <div class="daisy-flex daisy-flex-wrap daisy-gap-2 daisy-items-center">
      <button
        type="button"
        class="daisy-btn daisy-btn-primary"
        data-testid="run-data-migration-button"
        :disabled="runInFlight"
        @click="runMigrationStub"
      >
        Run migration
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from "vue"
import { AdminDataMigrationController } from "@generated/doughnut-backend-api/sdk.gen"
import type { AdminDataMigrationStatusDto } from "@generated/doughnut-backend-api/types.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"

const MIGRATION_BATCH_SAFETY_LIMIT = 500

const error = ref<string | undefined>(undefined)
const status = ref<AdminDataMigrationStatusDto | undefined>(undefined)
const runInFlight = ref(false)

const summaryLine = computed(() => {
  const msg = status.value?.message?.trim()
  if (!msg) {
    return "Loading…"
  }
  return msg
})

const hasProgressDetail = computed(() => {
  const d = status.value
  if (!d) {
    return false
  }
  if (d.lastError) {
    return true
  }
  return Boolean(
    d.currentStepName ||
      d.stepStatus ||
      (d.totalCount !== undefined && d.totalCount > 0) ||
      (d.processedCount !== undefined && d.processedCount > 0)
  )
})

const loadStatus = async () => {
  error.value = undefined
  const { data, error: apiError } =
    await AdminDataMigrationController.getAdminDataMigrationStatus({})
  if (apiError) {
    error.value = toOpenApiError(apiError).message ?? "Request failed"
    return
  }
  if (data) {
    status.value = data
  }
}

onMounted(async () => {
  await loadStatus()
})

function migrationFinished(d: AdminDataMigrationStatusDto): boolean {
  return Boolean(d.dataMigrationComplete) || d.stepStatus === "FAILED"
}

const runMigrationStub = async () => {
  error.value = undefined
  runInFlight.value = true
  try {
    const { data, error: apiError } = await apiCallWithLoading(async () => {
      let lastResult = await AdminDataMigrationController.runDataMigrationBatch(
        {}
      )
      if (lastResult.error) {
        return lastResult
      }
      let current = lastResult.data
      status.value = current
      for (
        let i = 0;
        i < MIGRATION_BATCH_SAFETY_LIMIT &&
        current &&
        !migrationFinished(current);
        i++
      ) {
        lastResult = await AdminDataMigrationController.runDataMigrationBatch(
          {}
        )
        if (lastResult.error) {
          return lastResult
        }
        current = lastResult.data
        status.value = current
      }
      return { data: current, error: undefined }
    })
    if (apiError) {
      error.value = toOpenApiError(apiError).message ?? "Request failed"
      return
    }
    if (data) {
      status.value = data
      if (!migrationFinished(data)) {
        error.value = "Migration stopped: batch safety limit exceeded."
      }
    }
  } finally {
    runInFlight.value = false
  }
}
</script>

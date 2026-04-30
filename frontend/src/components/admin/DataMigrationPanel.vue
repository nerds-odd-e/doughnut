<template>
  <div aria-live="polite">
    <div class="daisy-alert daisy-alert-info daisy-mb-4" role="status">
      Admin data migrations will appear here once a migration is implemented on the server. The stub
      below shows whatever the backend returns today.
    </div>

    <div v-if="error" class="daisy-alert daisy-alert-error daisy-mb-4" role="alert">
      {{ error }}
    </div>

    <div class="daisy-mb-4" data-testid="data-migration-status">
      <p class="daisy-text-sm daisy-opacity-90">
        {{ summaryLine }}
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

const runMigrationStub = async () => {
  error.value = undefined
  runInFlight.value = true
  try {
    const { data, error: apiError } = await apiCallWithLoading(() =>
      AdminDataMigrationController.runDataMigrationBatch({})
    )
    if (apiError) {
      error.value = toOpenApiError(apiError).message ?? "Request failed"
      return
    }
    if (data) {
      status.value = data
    }
  } finally {
    runInFlight.value = false
  }
}
</script>

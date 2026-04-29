<template>
  <div aria-live="polite">
    <div
      class="daisy-alert daisy-alert-info daisy-mb-4"
      role="status"
    >
      Data migrations run on the server. Use Run migration to detach legacy index-folder
      topology and rebuild wiki slugs. Large datasets may take a minute.
      Last status is retained for this running server—you can reload the page later.
    </div>

    <div v-if="error" class="daisy-alert daisy-alert-error daisy-mb-4" role="alert">
      {{ error }}
    </div>

    <div class="daisy-mb-4" data-testid="data-migration-status">
      <p class="daisy-text-sm daisy-opacity-90 daisy-mb-2">
        {{ summaryLine }}
      </p>
      <p class="daisy-text-sm daisy-opacity-75">
        Last run: {{ lastRunLabel }}
      </p>
    </div>

    <div class="daisy-flex daisy-flex-wrap daisy-gap-2 daisy-items-center">
      <button
        type="button"
        class="daisy-btn daisy-btn-primary"
        data-testid="run-data-migration-button"
        @click="runMigration"
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

const summaryLine = computed(() => {
  const s = status.value
  if (!s?.message) {
    return "Loading migration status…"
  }
  return s.message
})

const lastRunLabel = computed(() => {
  const at = status.value?.lastCompletedAt
  if (!at) {
    return "—"
  }
  const d = new Date(at)
  if (Number.isNaN(d.getTime())) {
    return "—"
  }
  return d.toLocaleString()
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

const runMigration = async () => {
  error.value = undefined
  const { data, error: apiError } = await apiCallWithLoading(() =>
    AdminDataMigrationController.runDataMigration({})
  )
  if (apiError) {
    error.value = toOpenApiError(apiError).message ?? "Request failed"
    return
  }
  if (data) {
    status.value = data
  }
}
</script>

<template>
  <div aria-live="polite">
    <div
      class="daisy-alert daisy-alert-info daisy-mb-4"
      role="status"
    >
      Data migrations run on the server in small HTTP batches—each batch completes one transactional
      step so long runs stay inside proxy deadlines. Progress updates after every batch. This
      migration detaches legacy index-folder topology and rebuilds wiki slugs.
      Last status is retained for this running server—you can reload the page later.
    </div>

    <div v-if="error" class="daisy-alert daisy-alert-error daisy-mb-4" role="alert">
      {{ error }}
    </div>

    <div class="daisy-mb-4" data-testid="data-migration-status">
      <p class="daisy-text-sm daisy-opacity-90 daisy-mb-2">
        {{ summaryLine }}
      </p>
      <p
        v-if="showPhaseLine"
        class="daisy-text-sm daisy-font-medium daisy-opacity-95 daisy-mb-2"
      >
        {{ phaseLine }}
      </p>
      <progress
        v-if="showProgressBar"
        class="daisy-progress daisy-progress-primary daisy-w-full daisy-mb-3"
        :value="progressValue"
        :max="progressMax"
        data-testid="data-migration-progress"
      ></progress>
      <p class="daisy-text-sm daisy-opacity-75">
        Last run: {{ lastRunLabel }}
      </p>
    </div>

    <div class="daisy-flex daisy-flex-wrap daisy-gap-2 daisy-items-center">
      <button
        type="button"
        class="daisy-btn daisy-btn-primary"
        data-testid="run-data-migration-button"
        :disabled="runDisabled"
        @click="runMigration"
      >
        Run migration
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, nextTick, onMounted, ref } from "vue"
import { AdminDataMigrationController } from "@generated/doughnut-backend-api/sdk.gen"
import type { AdminDataMigrationStatusDto } from "@generated/doughnut-backend-api/types.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"

const error = ref<string | undefined>(undefined)
const status = ref<AdminDataMigrationStatusDto | undefined>(undefined)
const runInFlight = ref(false)

const summaryLine = computed(() => {
  const s = status.value
  if (!s?.message) {
    return "Loading migration status…"
  }
  return s.message
})

const showPhaseLine = computed(() => {
  const s = status.value
  return Boolean(
    s?.migrationInProgress && (s.batchPhaseSummary?.trim().length ?? 0) > 0
  )
})

const phaseLine = computed(() => status.value?.batchPhaseSummary ?? "")

const progressMax = computed(() =>
  Math.max(1, status.value?.batchTotalPlanned ?? 1)
)

const progressValue = computed(() => status.value?.completedBatchOrdinal ?? 0)

const showProgressBar = computed(() => {
  const s = status.value
  return Boolean(s?.migrationInProgress && (s.batchTotalPlanned ?? 0) > 1)
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

const runDisabled = computed(() => runInFlight.value)

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

const mergeStatus = (dto: AdminDataMigrationStatusDto) => {
  status.value = dto
}

const runMigration = async () => {
  error.value = undefined
  runInFlight.value = true
  try {
    const outer = await apiCallWithLoading(async () => {
      let r = await AdminDataMigrationController.runDataMigrationBatch({})
      while (!r.error && r.data) {
        mergeStatus(r.data)
        await nextTick()
        if (!r.data.moreBatchesRemain) {
          break
        }
        r = await AdminDataMigrationController.runDataMigrationBatch({})
      }
      return r
    })

    if (outer.error) {
      error.value = toOpenApiError(outer.error).message ?? "Request failed"
    } else if (outer.data) {
      mergeStatus(outer.data)
    }
  } finally {
    runInFlight.value = false
  }
}
</script>

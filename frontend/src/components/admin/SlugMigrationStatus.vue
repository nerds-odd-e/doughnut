<template>
  <div aria-live="polite">
    <div
      class="daisy-alert daisy-alert-info daisy-mb-4"
      role="status"
    >
      This migration runs in bounded batches on the server. It may take a while on large
      notebooks. Progress is saved in the database—you can refresh or leave this page and use
      the migration controls later to continue until no rows remain.
    </div>

    <div v-if="error" class="daisy-alert daisy-alert-error daisy-mb-4" role="alert">
      {{ error }}
    </div>

    <div v-if="stuckMessage" class="daisy-alert daisy-alert-error daisy-mb-4" role="alert">
      {{ stuckMessage }}
    </div>

    <div v-if="status">
      <div :aria-busy="migrationRunning ? 'true' : 'false'">
        <dl class="daisy-stats daisy-stats-vertical lg:daisy-stats-horizontal daisy-shadow">
          <div class="daisy-stat">
            <div class="daisy-stat-title">Folders missing slug</div>
            <div class="daisy-stat-value">{{ foldersRemaining }}</div>
          </div>
          <div class="daisy-stat">
            <div class="daisy-stat-title">Notes missing slug</div>
            <div class="daisy-stat-value">{{ notesRemaining }}</div>
          </div>
        </dl>

        <p v-if="migrationRunning" class="daisy-text-sm daisy-mb-2 daisy-mt-4">
          Migrating…
        </p>
        <p v-else-if="lastProcessedLabel" class="daisy-text-sm daisy-mb-2 daisy-mt-4 daisy-opacity-90">
          {{ lastProcessedLabel }}
        </p>

        <div v-if="migrationCompleteNoRun" class="daisy-alert daisy-alert-success daisy-mt-4">
          Slug migration is complete. No rows are missing slug data.
        </div>

        <div v-else class="daisy-flex daisy-flex-wrap daisy-gap-2 daisy-items-center daisy-mt-4">
          <button
            type="button"
            class="daisy-btn daisy-btn-primary"
            :disabled="migrationRunning || initialLoadFailed"
            @click="runMigration"
          >
            Run migration
          </button>
          <button
            v-if="migrationRunning"
            type="button"
            class="daisy-btn daisy-btn-ghost"
            @click="requestStopMigration"
          >
            Stop
          </button>
        </div>
      </div>
    </div>
    <div v-else-if="loadingInitial">Loading slug migration status...</div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue"
import { WikiSlugMigrationAdminController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { WikiSlugMigrationStatus } from "@generated/doughnut-backend-api"

const BATCH_LIMIT = 100

const status = ref<WikiSlugMigrationStatus | undefined>(undefined)
const error = ref<string | undefined>(undefined)
const stuckMessage = ref<string | undefined>(undefined)
const migrationRunning = ref(false)
const cancelMigration = ref(false)
const lastProcessedInBatch = ref<number | undefined>(undefined)
const lastBatchKind = ref<"folders" | "notes" | undefined>(undefined)
const initialLoadFailed = ref(false)
const loadingInitial = ref(true)

const foldersRemaining = computed(() => status.value?.foldersMissingSlug ?? 0)
const notesRemaining = computed(() => status.value?.notesMissingSlug ?? 0)

const hasRemainingWork = computed(
  () => foldersRemaining.value > 0 || notesRemaining.value > 0
)

const migrationCompleteNoRun = computed(
  () => !hasRemainingWork.value && !migrationRunning.value
)

const lastProcessedLabel = computed(() => {
  if (migrationRunning.value || lastProcessedInBatch.value === undefined) {
    return ""
  }
  const kind =
    lastBatchKind.value === "folders"
      ? "folders"
      : lastBatchKind.value === "notes"
        ? "notes"
        : "rows"
  return `Last batch processed ${lastProcessedInBatch.value} ${kind}.`
})

const applyBatchResult = (
  processed: number | undefined,
  newStatus: WikiSlugMigrationStatus | undefined,
  kind: "folders" | "notes"
) => {
  lastProcessedInBatch.value = processed ?? 0
  lastBatchKind.value = kind
  if (newStatus) {
    status.value = newStatus
  }
}

const fetchStatus = async () => {
  loadingInitial.value = true
  error.value = undefined
  const { data, error: err } = await apiCallWithLoading(() =>
    WikiSlugMigrationAdminController.getStatus()
  )
  loadingInitial.value = false
  if (err) {
    error.value = String(err)
    initialLoadFailed.value = true
  } else if (data) {
    status.value = data
    initialLoadFailed.value = false
  }
}

const requestStopMigration = () => {
  cancelMigration.value = true
}

const runMigration = async () => {
  error.value = undefined
  stuckMessage.value = undefined
  cancelMigration.value = false
  migrationRunning.value = true

  const runFolders = async (): Promise<boolean> => {
    while (
      !cancelMigration.value &&
      (status.value?.foldersMissingSlug ?? 0) > 0
    ) {
      const { data, error: err } = await apiCallWithLoading(() =>
        WikiSlugMigrationAdminController.batchMigrateFolders({
          query: { limit: BATCH_LIMIT },
        })
      )
      if (cancelMigration.value) {
        return false
      }
      if (err) {
        error.value = String(err)
        return false
      }
      const processed = data?.processedInBatch ?? 0
      const next = data?.status
      applyBatchResult(processed, next, "folders")
      if (processed === 0 && (status.value?.foldersMissingSlug ?? 0) > 0) {
        stuckMessage.value =
          "Migration made no progress on folders while some folders still lack a slug. Check data or try again."
        return false
      }
    }
    return true
  }

  const runNotes = async (): Promise<boolean> => {
    while (
      !cancelMigration.value &&
      (status.value?.notesMissingSlug ?? 0) > 0
    ) {
      const { data, error: err } = await apiCallWithLoading(() =>
        WikiSlugMigrationAdminController.batchMigrateNotes({
          query: { limit: BATCH_LIMIT },
        })
      )
      if (cancelMigration.value) {
        return false
      }
      if (err) {
        error.value = String(err)
        return false
      }
      const processed = data?.processedInBatch ?? 0
      const next = data?.status
      applyBatchResult(processed, next, "notes")
      if (processed === 0 && (status.value?.notesMissingSlug ?? 0) > 0) {
        stuckMessage.value =
          "Migration made no progress on notes while some notes still lack a slug. Check data or try again."
        return false
      }
    }
    return true
  }

  try {
    const foldersOk = await runFolders()
    if (!foldersOk) {
      return
    }
    await runNotes()
  } finally {
    migrationRunning.value = false
  }
}

onBeforeUnmount(() => {
  cancelMigration.value = true
})

onMounted(() => {
  fetchStatus()
})
</script>

<template>
  <div>
    <div
      v-if="!error"
      class="daisy-alert daisy-alert-info daisy-mb-4"
      role="status"
      aria-live="polite"
    >
      This migration runs in bounded batches on the server. It may take a while on large
      notebooks. Progress is saved in the database—you can refresh or leave this page and use
      the migration controls later to continue until no rows remain.
    </div>
    <div v-if="status">
      <dl class="daisy-stats daisy-stats-vertical lg:daisy-stats-horizontal daisy-shadow">
        <div class="daisy-stat">
          <div class="daisy-stat-title">Folders missing slug</div>
          <div class="daisy-stat-value">{{ status.foldersMissingSlug }}</div>
        </div>
        <div class="daisy-stat">
          <div class="daisy-stat-title">Notes missing slug</div>
          <div class="daisy-stat-value">{{ status.notesMissingSlug }}</div>
        </div>
      </dl>
    </div>
    <div v-else-if="error">
      {{ error }}
    </div>
    <div v-else>Loading slug migration status...</div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from "vue"
import { WikiSlugMigrationAdminController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { WikiSlugMigrationStatus } from "@generated/doughnut-backend-api"

const status = ref<WikiSlugMigrationStatus | undefined>(undefined)
const error = ref<string | undefined>(undefined)

const fetchStatus = async () => {
  const { data, error: err } = await apiCallWithLoading(() =>
    WikiSlugMigrationAdminController.getStatus()
  )
  if (err) {
    error.value = String(err)
  } else if (data) {
    status.value = data
  }
}

onMounted(() => {
  fetchStatus()
})
</script>

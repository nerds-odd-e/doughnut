<template>
  <div aria-live="polite">
    <div
      class="daisy-alert daisy-alert-info daisy-mb-4"
      role="status"
    >
      Data migrations run in bounded batches on the server. Large datasets may take a while.
      Progress is stored server-side—you can leave this page and return later to continue.
    </div>

    <div v-if="error" class="daisy-alert daisy-alert-error daisy-mb-4" role="alert">
      {{ error }}
    </div>

    <div class="daisy-mb-4">
      <p class="daisy-text-sm daisy-opacity-90 daisy-mb-2">
        {{ statusLine }}
      </p>
      <p class="daisy-text-sm daisy-opacity-75">
        Last run: {{ lastRunLabel }}
      </p>
    </div>

    <div class="daisy-flex daisy-flex-wrap daisy-gap-2 daisy-items-center">
      <button
        type="button"
        class="daisy-btn daisy-btn-primary"
        @click="runMigration"
      >
        Run migration
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue"

const error = ref<string | undefined>(undefined)

const statusLine = "No migration job is running."
const lastRunLabel = "—"

const runMigration = () => {
  // Stub until wired to admin-only migration endpoint.
}
</script>

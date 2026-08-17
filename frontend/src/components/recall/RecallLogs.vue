<script setup lang="ts">
import type { RecallLog } from "@generated/doughnut-backend-api"

defineProps<{
  recallLogs: RecallLog[]
}>()
</script>

<template>
  <section v-if="recallLogs.length > 0" data-testid="recall-logs" class="mb-6">
    <h2 class="daisy-card-title text-lg mb-4">Recall Logs</h2>
    <div
      v-for="log in recallLogs"
      :key="log.id"
      data-testid="recall-log"
      class="daisy-card shadow-sm mb-2"
    >
      <div class="daisy-card-body text-sm flex flex-wrap gap-4">
        <span data-testid="recall-log-product-outcome">{{ log.productOutcome }}</span>
        <span>Recorded: {{ new Date(log.recordedAt).toLocaleString() }}</span>
        <span v-if="log.elapsedHours != null">
          Elapsed hours: {{ log.elapsedHours }}
        </span>
      </div>
    </div>
  </section>
</template>

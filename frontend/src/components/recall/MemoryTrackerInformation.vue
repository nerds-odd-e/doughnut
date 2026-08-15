<template>
  <div class="daisy-card shadow-sm mb-6">
    <div class="daisy-card-body">
      <h2 class="daisy-card-title text-lg mb-4">Memory Tracker Information</h2>
      <div class="grid grid-cols-2 gap-4 text-sm">
        <div>
          <span class="font-semibold">Assimilated Time:</span>
          <span class="ml-2">
            {{
              memoryTracker.assimilatedAt
                ? new Date(memoryTracker.assimilatedAt).toLocaleString()
                : "N/A"
            }}
          </span>
        </div>
        <div>
          <span class="font-semibold">Last Recall Time:</span>
          <span class="ml-2">
            {{
              memoryTracker.lastRecalledAt
                ? new Date(memoryTracker.lastRecalledAt).toLocaleString()
                : "N/A"
            }}
          </span>
        </div>
        <div>
          <span class="font-semibold">Next Recall Time:</span>
          <span class="ml-2">
            {{ new Date(memoryTracker.nextRecallAt).toLocaleString() }}
          </span>
        </div>
        <div>
          <span class="font-semibold">Stability:</span>
          <span class="ml-2">
            {{ memoryTracker.stability ?? "N/A" }}
          </span>
        </div>
        <div>
          <span class="font-semibold">Recall Count:</span>
          <span class="ml-2">
            {{ memoryTracker.recallCount ?? "N/A" }}
          </span>
        </div>
        <div>
          <span class="font-semibold">Type:</span>
          <span class="ml-2">
            {{ memoryTracker.type }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { MemoryTracker } from "@generated/doughnut-backend-api"

defineProps<{
  memoryTracker: MemoryTracker
}>()
</script>

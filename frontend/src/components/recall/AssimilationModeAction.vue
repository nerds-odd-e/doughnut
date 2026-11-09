<template>
  <span
    v-if="loading"
    class="daisy-loading daisy-loading-spinner daisy-loading-xs"
    role="status"
    aria-label="Loading tracker information"
    data-app-busy="true"
  />
  <template v-else-if="tracker">
    <router-link
      :to="{
        name: 'memoryTrackerShow',
        params: { memoryTrackerId: tracker.id },
      }"
      class="daisy-link daisy-link-primary shrink-0 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2"
      :data-test="`assimilation-status-${mode}`"
      :title="`Recalled ${tracker.recallCount ?? 0} times`"
    >
      View tracker
    </router-link>
    <span class="text-base-content/60 shrink-0 text-xs">
      Next {{ nextRecallAtText }}
    </span>
  </template>
  <slot v-else />
</template>

<script setup lang="ts">
import type { MemoryTracker } from "@generated/donut-backend-api"
import { computed } from "vue"
import type { MemoryTrackerType } from "./assimilationMemoryTrackers"

const props = defineProps<{
  mode: MemoryTrackerType
  tracker?: MemoryTracker
  loading: boolean
}>()

const nextRecallAtText = computed(() =>
  props.tracker
    ? new Date(props.tracker.nextRecallAt).toLocaleDateString(undefined, {
        day: "numeric",
        month: "short",
      })
    : undefined
)
</script>

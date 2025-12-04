<template>
  <button
    @click="navigateToMemoryTracker"
    class="daisy-btn daisy-btn-primary daisy-mt-4"
  >
    View Memory Tracker
  </button>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router"

const props = defineProps({
  memoryTrackerId: {
    type: Number,
    required: true,
  },
})

const router = useRouter()

function navigateToMemoryTracker() {
  router.push({
    name: "memoryTrackerShow",
    params: { memoryTrackerId: props.memoryTrackerId },
  })
}
</script>


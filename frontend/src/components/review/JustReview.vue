<template>
  <MemoryTrackerAsync
    v-if="memoryTrackerId"
    v-bind="{
      memoryTrackerId,
    }"
  />
  <SelfEvaluateButtons
    @self-evaluated-memory-state="justReivew($event)"
    :key="memoryTrackerId"
  />
</template>

<script setup lang="ts">
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import MemoryTrackerAsync from "./MemoryTrackerAsync.vue"
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue"

const props = defineProps({
  memoryTrackerId: Number,
})

const emit = defineEmits<{
  reviewed: []
}>()

const justReivew = async (successful: boolean) => {
  if (props.memoryTrackerId === undefined) {
    return
  }
  const { error } = await apiCallWithLoading(() =>
    MemoryTrackerController.markAsRepeated({
      path: { memoryTracker: props.memoryTrackerId! },
      query: { successful },
    })
  )
  if (!error) {
    emit("reviewed")
  }
}
</script>

<template>
  <ContainerPage
    v-bind="{ contentLoaded: error || (recallPrompts !== undefined && memoryTracker !== undefined), title: 'Memory Tracker' }"
  >
    <div v-if="error" class="daisy-alert daisy-alert-error">
      Error loading memory tracker data
    </div>
    <ContentLoader v-else-if="recallPrompts === undefined || memoryTracker === undefined" />
    <MemoryTrackerPageView
      v-else-if="recallPrompts !== undefined && memoryTracker !== undefined"
      :recall-prompts="recallPrompts"
      :memory-tracker="memoryTracker"
      :memory-tracker-id="memoryTrackerId"
      @refresh="fetchData"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { RecallPrompt, MemoryTracker } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import MemoryTrackerPageView from "./MemoryTrackerPageView.vue"

const props = defineProps<{
  memoryTrackerId: number
}>()

const recallPrompts = ref<RecallPrompt[] | undefined>(undefined)
const memoryTracker = ref<MemoryTracker | undefined>(undefined)
const error = ref(false)

const fetchData = async () => {
  const [promptsResult, trackerResult] = await Promise.all([
    MemoryTrackerController.getRecallPrompts({
      path: { memoryTracker: props.memoryTrackerId },
    }),
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: props.memoryTrackerId },
    }),
  ])

  if (promptsResult.error || trackerResult.error) {
    error.value = true
  } else {
    recallPrompts.value = promptsResult.data ?? []
    memoryTracker.value = trackerResult.data
  }
}

onMounted(() => {
  fetchData()
})
</script>


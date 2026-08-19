<template>
  <ContainerPage
    v-bind="{ contentLoaded: error || pageData !== undefined, title: 'Memory Tracker' }"
  >
    <div v-if="error" class="daisy-alert daisy-alert-error">
      Error loading memory tracker data
    </div>
    <ContentLoader v-else-if="pageData === undefined" />
    <MemoryTrackerPageView
      v-else
      :recall-history="pageData.recallHistory"
      :memory-tracker="pageData.memoryTracker"
      :memory-tracker-id="memoryTrackerId"
      @refresh="fetchData"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from "vue"
import type {
  RecallHistoryItem,
  MemoryTracker,
} from "@generated/doughnut-backend-api"
import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import MemoryTrackerPageView from "./MemoryTrackerPageView.vue"

const props = defineProps<{
  memoryTrackerId: number
}>()

const recallHistory = ref<RecallHistoryItem[] | undefined>(undefined)
const memoryTracker = ref<MemoryTracker | undefined>(undefined)
const error = ref(false)
const pageData = computed(() => {
  if (recallHistory.value === undefined || memoryTracker.value === undefined) {
    return undefined
  }
  return {
    recallHistory: recallHistory.value,
    memoryTracker: memoryTracker.value,
  }
})

const fetchData = async () => {
  const [historyResult, trackerResult] = await Promise.all([
    MemoryTrackerController.getRecallHistory({
      path: { memoryTracker: props.memoryTrackerId },
    }),
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: props.memoryTrackerId },
    }),
  ])

  if (historyResult.error || trackerResult.error) {
    error.value = true
  } else {
    recallHistory.value = historyResult.data ?? []
    memoryTracker.value = trackerResult.data
  }
}

onMounted(() => {
  fetchData()
})
</script>

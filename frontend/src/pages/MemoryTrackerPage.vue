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
      :recall-prompts="pageData.recallPrompts"
      :recall-logs="pageData.recallLogs"
      :memory-tracker="pageData.memoryTracker"
      :memory-tracker-id="memoryTrackerId"
      @refresh="fetchData"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from "vue"
import type {
  RecallPromptHistoryItem,
  MemoryTracker,
  RecallLog,
} from "@generated/doughnut-backend-api"
import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import MemoryTrackerPageView from "./MemoryTrackerPageView.vue"

const props = defineProps<{
  memoryTrackerId: number
}>()

const recallPrompts = ref<RecallPromptHistoryItem[] | undefined>(undefined)
const memoryTracker = ref<MemoryTracker | undefined>(undefined)
const recallLogs = ref<RecallLog[] | undefined>(undefined)
const error = ref(false)
const pageData = computed(() => {
  if (
    recallPrompts.value === undefined ||
    memoryTracker.value === undefined ||
    recallLogs.value === undefined
  ) {
    return undefined
  }
  return {
    recallPrompts: recallPrompts.value,
    memoryTracker: memoryTracker.value,
    recallLogs: recallLogs.value,
  }
})

const fetchData = async () => {
  const [promptsResult, trackerResult, logsResult] = await Promise.all([
    MemoryTrackerController.getRecallPrompts({
      path: { memoryTracker: props.memoryTrackerId },
    }),
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: props.memoryTrackerId },
    }),
    MemoryTrackerController.getRecallLogs({
      path: { memoryTracker: props.memoryTrackerId },
    }),
  ])

  if (promptsResult.error || trackerResult.error || logsResult.error) {
    error.value = true
  } else {
    recallPrompts.value = promptsResult.data ?? []
    memoryTracker.value = trackerResult.data
    recallLogs.value = logsResult.data ?? []
  }
}

onMounted(() => {
  fetchData()
})
</script>

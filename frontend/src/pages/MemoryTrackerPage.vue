<template>
  <ContainerPage
    v-bind="{ contentLoaded: recallPrompts !== undefined, title: 'Memory Tracker' }"
  >
    <ContentLoader v-if="recallPrompts === undefined && !error" />
    <div v-else-if="error" class="daisy-alert daisy-alert-error">
      Error loading recall prompts
    </div>
    <MemoryTrackerPageView
      v-else-if="recallPrompts !== undefined"
      :recall-prompts="recallPrompts"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { RecallPrompt } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import MemoryTrackerPageView from "./MemoryTrackerPageView.vue"

const props = defineProps<{
  memoryTrackerId: number
}>()

const recallPrompts = ref<RecallPrompt[] | undefined>(undefined)
const error = ref(false)

const fetchData = async () => {
  const { data: prompts, error: fetchError } =
    await MemoryTrackerController.getRecallPrompts({
      path: { memoryTracker: props.memoryTrackerId },
    })
  if (fetchError) {
    error.value = true
    recallPrompts.value = []
  } else {
    recallPrompts.value = prompts ?? []
  }
}

onMounted(() => {
  fetchData()
})
</script>


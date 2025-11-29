<template>
  <ContainerPage
    v-bind="{ contentLoaded: answeredQuestion !== undefined, title: 'Memory Tracker' }"
  >
    <ContentLoader v-if="answeredQuestion === undefined && !error" />
    <div v-else-if="error" class="daisy-alert daisy-alert-error">
      Error loading answered question
    </div>
    <MemoryTrackerPageView
      v-else-if="answeredQuestion !== undefined"
      :answered-question="answeredQuestion"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { AnsweredQuestion } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import MemoryTrackerPageView from "./MemoryTrackerPageView.vue"

const props = defineProps<{
  memoryTrackerId: number
}>()

const answeredQuestion = ref<AnsweredQuestion | null | undefined>(undefined)
const error = ref(false)

const fetchData = async () => {
  const { data: question, error: fetchError } =
    await MemoryTrackerController.getLastAnsweredQuestion({
      path: { memoryTracker: props.memoryTrackerId },
    })
  if (fetchError) {
    error.value = true
    answeredQuestion.value = null
  } else {
    answeredQuestion.value = question ?? null
  }
}

onMounted(() => {
  fetchData()
})
</script>


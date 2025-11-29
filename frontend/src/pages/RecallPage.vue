<template>
  <RecallPageView
    v-if="toRepeat !== undefined"
    :to-repeat="toRepeat"
    :current-index="currentIndex"
    :total-assimilated-count="totalAssimilatedCount ?? 0"
    :eager-fetch-count="eagerFetchCount"
    :storage-accessor="storageAccessor"
    @answered-question="onAnsweredQuestion"
    @answered-spelling="onAnsweredSpelling"
    @just-reviewed="onJustReviewed"
    @move-to-end="moveMemoryTrackerToEnd"
    @load-more="loadMore"
  />
</template>

<script setup lang="ts">
import type { MemoryTrackerLite } from "@generated/backend"
import { RecallsController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import getEnvironment from "@/managedApi/window/getEnvironment"
import timezoneParam from "@/managedApi/window/timezoneParam"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { shuffle } from "es-toolkit"
import type { PropType } from "vue"
import { onMounted, ref, onActivated } from "vue"
import { useRecallData } from "@/composables/useRecallData"
import RecallPageView from "./RecallPageView.vue"

const {
  setToRepeatCount,
  recallWindowEndAt,
  setRecallWindowEndAt,
  totalAssimilatedCount,
} = useRecallData()

defineProps({
  eagerFetchCount: Number,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const toRepeat = ref<MemoryTrackerLite[] | undefined>(undefined)
const currentIndex = ref(0)

const loadMore = async (dueInDays?: number) => {
  const { data: response, error } = await RecallsController.recalling({
    query: {
      timezone: timezoneParam(),
      dueindays: dueInDays,
    },
  })
  if (!error && response) {
    toRepeat.value = response.toRepeat
    currentIndex.value = 0
    if (toRepeat.value?.length === 0) {
      return response
    }
    if (getEnvironment() !== "testing" && toRepeat.value) {
      toRepeat.value = shuffle(toRepeat.value)
    }
    return response
  }
  return undefined
}

const onAnsweredQuestion = () => {
  currentIndex.value += 1
}

const onAnsweredSpelling = () => {
  currentIndex.value += 1
}

const onJustReviewed = () => {
  currentIndex.value += 1
}

const moveMemoryTrackerToEnd = (index: number) => {
  const currentToRepeat = toRepeat.value
  if (!currentToRepeat) return

  const item = currentToRepeat[index]
  if (item === undefined) return
  toRepeat.value = [
    ...currentToRepeat.slice(0, index),
    ...currentToRepeat.slice(index + 1),
    item,
  ]
}

const loadCurrentDueRecalls = async () => {
  toRepeat.value = undefined
  const response = await loadMore(0)
  if (response) {
    setToRepeatCount(response.toRepeatCount)
    setRecallWindowEndAt(response.recallWindowEndAt)
  }
}

onMounted(() => {
  loadCurrentDueRecalls()
})

onActivated(() => {
  const currentTime = new Date().toISOString()
  if (recallWindowEndAt.value && currentTime > recallWindowEndAt.value) {
    loadCurrentDueRecalls()
  }
})

defineExpose({
  toRepeat,
  currentIndex,
})
</script>

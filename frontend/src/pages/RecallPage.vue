<template>
  <RecallProgressBar
    v-if="isProgressBarVisible"
    v-bind="{
      finished,
      toRepeatCount,
      previousResultCursor,
    }"
    @view-last-result="viewLastResult($event)"
    @show-more="showTooltip = true"
  >
  </RecallProgressBar>

  <div v-if="showTooltip" class="tooltip-popup" @click="showTooltip = false">
    <div class="tooltip-content">
      <p>Daily Progress: {{ finished }} / {{ finished + toRepeatCount }}</p>
      <p>Total assimilated: {{ finished }} / {{ totalCount }}</p>
    </div>
  </div>

  <template v-if="toRepeat != undefined">
    <Quiz
      v-if="toRepeatCount !== 0"
      v-show="!currentResult"
      :memory-trackers="toRepeat"
      :current-index="currentIndex"
      :eager-fetch-count="eagerFetchCount ?? 5"
      :storage-accessor="storageAccessor"
      @answered="onAnswered($event)"
      @move-to-end="moveMemoryTrackerToEnd"
    />
    <AnsweredQuestionComponent
      v-if="currentResult"
      v-bind="{ answeredQuestion: currentResult, storageAccessor }"
    />
    <template v-else-if="toRepeatCount === 0">
      <div class="alert alert-success">
        You have finished all repetitions for this half a day!
      </div>
      <div>
        <button role="button" class="btn btn-secondary" @click="loadMore(3)">
          Load more from next 3 days
        </button>
        <button role="button" class="btn btn-secondary" @click="loadMore(7)">
          Load more from next 7 days
        </button>
        <button role="button" class="btn btn-secondary" @click="loadMore(14)">
          Load more from next 14 days
        </button>
      </div>
    </template>
  </template>
</template>

<script setup lang="ts">
import Quiz from "@/components/review/Quiz.vue"
import RecallProgressBar from "@/components/review/RecallProgressBar.vue"
import AnsweredQuestionComponent from "@/components/review/AnsweredQuestionComponent.vue"
import type { AnsweredQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import getEnvironment from "@/managedApi/window/getEnvironment"
import timezoneParam from "@/managedApi/window/timezoneParam"
import type { StorageAccessor } from "@/store/createNoteStorage"
import _ from "lodash"
import type { PropType } from "vue"
import { computed, onMounted, ref, onActivated, onDeactivated } from "vue"
import { useRecallData } from "@/composables/useRecallData"

const { managedApi } = useLoadingApi()
const {
  setToRepeatCount,
  decrementToRepeatCount,
  recallWindowEndAt,
  setRecallWindowEndAt,
} = useRecallData()
defineProps({
  eagerFetchCount: Number,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const toRepeat = ref<number[] | undefined>(undefined)
const currentIndex = ref(0)
const previousResults = ref<(AnsweredQuestion | undefined)[]>([])
const previousResultCursor = ref<number | undefined>(undefined)
const isProgressBarVisible = ref(true)
const showTooltip = ref(false)

const currentResult = computed(() => {
  if (previousResultCursor.value === undefined) return undefined
  return previousResults.value[previousResultCursor.value]
})

const finished = computed(() => previousResults.value.length)
const toRepeatCount = computed(
  () => (toRepeat.value?.length ?? 0) - currentIndex.value
)
const totalCount = computed(
  () => (toRepeat.value?.length ?? 0) + finished.value
)

const viewLastResult = (cursor: number | undefined) => {
  previousResultCursor.value = cursor
}

const loadMore = async (dueInDays?: number) => {
  const response = await managedApi.restRecallsController.recalling(
    timezoneParam(),
    dueInDays
  )
  toRepeat.value = response.toRepeat
  currentIndex.value = 0
  if (toRepeat.value?.length === 0) {
    return response
  }
  if (getEnvironment() !== "testing") {
    toRepeat.value = _.shuffle(toRepeat.value)
  }
  return response
}

const onAnswered = (answerResult: AnsweredQuestion) => {
  currentIndex.value += 1
  previousResults.value.push(answerResult)
  decrementToRepeatCount()
  if (!answerResult) return
  if (!answerResult.answer.correct) {
    viewLastResult(previousResults.value.length - 1)
  }
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
  setToRepeatCount(response.toRepeatCount)
  setRecallWindowEndAt(response.recallWindowEndAt)
}

onMounted(() => {
  loadCurrentDueRecalls()
})

onActivated(() => {
  isProgressBarVisible.value = true
  const currentTime = new Date().toISOString()
  if (recallWindowEndAt.value && currentTime > recallWindowEndAt.value) {
    loadCurrentDueRecalls()
  }
})

onDeactivated(() => {
  isProgressBarVisible.value = false
})

defineExpose({
  toRepeat,
  currentIndex,
})
</script>

<style lang="scss" scoped>
.tooltip-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.tooltip-content {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  p {
    margin: 0.5rem 0;
    color: #333;
  }
}
</style>

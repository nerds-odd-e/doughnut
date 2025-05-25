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

  <div
    v-if="showTooltip"
    class="tooltip-popup daisy-fixed daisy-inset-0 daisy-bg-black/50 daisy-flex daisy-justify-center daisy-items-center daisy-z-[1000]"
    @click="showTooltip = false"
  >
    <div class="tooltip-content daisy-bg-white daisy-p-4 daisy-rounded-lg daisy-shadow-lg">
      <p class="daisy-my-2 daisy-text-neutral">Daily Progress: {{ finished }} / {{ finished + toRepeatCount }}</p>
      <p class="daisy-my-2 daisy-text-neutral">Total assimilated: {{ finished }} / {{ totalAssimilatedCount }}</p>
    </div>
  </div>

  <template v-if="toRepeat != undefined">
    <Quiz
      v-if="toRepeatCount !== 0"
      v-show="!currentAnsweredQuestion && !currentAnsweredSpelling"
      :memory-trackers="toRepeat"
      :current-index="currentIndex"
      :eager-fetch-count="eagerFetchCount ?? 5"
      :storage-accessor="storageAccessor"
      @answered-question="onAnsweredQuestion"
      @answered-spelling="onAnsweredSpelling"
      @just-reviewed="onJustReviewed"
      @move-to-end="moveMemoryTrackerToEnd"
    />
    <AnsweredQuestionComponent
      v-if="currentAnsweredQuestion"
      v-bind="{ answeredQuestion: currentAnsweredQuestion, conversationButton: true, storageAccessor }"
    />
    <AnsweredSpellingQuestion
      v-if="currentAnsweredSpelling"
      v-bind="{ result: currentAnsweredSpelling, storageAccessor }"
    />
    <template v-else-if="toRepeatCount === 0">
      <div class="daisy-alert daisy-alert-success">
        You have finished all repetitions for this half a day!
      </div>
      <div>
        <button role="button" class="daisy-btn daisy-btn-secondary" @click="loadMore(3)">
          Load more from next 3 days
        </button>
        <button role="button" class="daisy-btn daisy-btn-secondary" @click="loadMore(7)">
          Load more from next 7 days
        </button>
        <button role="button" class="daisy-btn daisy-btn-secondary" @click="loadMore(14)">
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
import AnsweredSpellingQuestion from "@/components/review/AnsweredSpellingQuestion.vue"
import type { AnsweredQuestion } from "generated/backend/models/AnsweredQuestion"
import type { SpellingResultDTO } from "generated/backend/models/SpellingResultDTO"
import type { MemoryTrackerLite } from "generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import getEnvironment from "@/managedApi/window/getEnvironment"
import timezoneParam from "@/managedApi/window/timezoneParam"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { shuffle } from "es-toolkit"
import type { PropType } from "vue"
import { computed, onMounted, ref, onActivated, onDeactivated } from "vue"
import { useRecallData } from "@/composables/useRecallData"

export type SpellingResult = SpellingResultDTO & { type: "spelling" }

export type QuestionResult = {
  type: "question"
  answeredQuestion: AnsweredQuestion
}

type RecallResult = QuestionResult | SpellingResult

const { managedApi } = useLoadingApi()
const {
  setToRepeatCount,
  decrementToRepeatCount,
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
const previousResults = ref<(RecallResult | undefined)[]>([])
const previousResultCursor = ref<number | undefined>(undefined)
const isProgressBarVisible = ref(true)
const showTooltip = ref(false)

const currentAnsweredQuestion = computed(() => {
  if (previousResultCursor.value === undefined) return undefined
  const result = previousResults.value[previousResultCursor.value]
  if (!result) return undefined
  return result.type === "question" ? result.answeredQuestion : undefined
})

const currentAnsweredSpelling = computed(() => {
  if (previousResultCursor.value === undefined) return undefined
  const result = previousResults.value[previousResultCursor.value]
  if (!result) return undefined
  return result.type === "spelling" ? result : undefined
})

const finished = computed(() => previousResults.value.length)
const toRepeatCount = computed(
  () => (toRepeat.value?.length ?? 0) - currentIndex.value
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
  if (getEnvironment() !== "testing" && toRepeat.value) {
    toRepeat.value = shuffle(toRepeat.value)
  }
  return response
}

const onAnsweredQuestion = (answerResult: AnsweredQuestion) => {
  currentIndex.value += 1
  previousResults.value.push({
    type: "question",
    answeredQuestion: answerResult,
  })
  if (!answerResult.answer.correct) {
    viewLastResult(previousResults.value.length - 1)
  }
  decrementToRepeatCount()
}

const onAnsweredSpelling = (answerResult: SpellingResultDTO) => {
  currentIndex.value += 1
  previousResults.value.push({
    type: "spelling",
    ...answerResult,
  })
  if (!answerResult.isCorrect) {
    viewLastResult(previousResults.value.length - 1)
  }
  decrementToRepeatCount()
}

const onJustReviewed = () => {
  currentIndex.value += 1
  previousResults.value.push(undefined)
  decrementToRepeatCount()
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

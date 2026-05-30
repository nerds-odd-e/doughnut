<template>
  <div class="recall-page h-full flex flex-col">
    <GlobalBar
      v-if="isProgressBarVisible"
      :class="[
        previousAnsweredQuestionCursor !== undefined ? 'repeat-paused' : '',
        treadmillMode ? 'treadmill-mode' : '',
      ]"
    >
      <RecallProgressBar
        v-bind="{
          finished,
          toRepeatCount,
          previousAnsweredQuestionCursor,
          canMoveToEnd: toRepeatCount > 0 && currentIndex < (toRepeat?.length ?? 0) - 1,
          currentIndex,
          totalAssimilatedCount: totalAssimilatedCount ?? 0,
          diligentMode: diligentMode,
          previousAnsweredQuestions,
        }"
        @view-last-answered-question="viewLastAnsweredQuestion($event)"
        @move-to-end="moveMemoryTrackerToEnd($event)"
        @treadmill-mode-changed="handleTreadmillModeChanged"
      >
      </RecallProgressBar>
    </GlobalBar>

    <div class="flex-1 min-h-0 overflow-y-auto">
    <template v-if="toRepeat != undefined">
      <Quiz
        v-if="toRepeatCount !== 0 && getCurrentMemoryTracker() && (!treadmillMode || !getCurrentMemoryTracker()?.spelling)"
        v-show="!currentAnsweredQuestion && !currentAnsweredSpelling"
        :memory-trackers="memoryTrackers"
        :current-index="getCurrentMemoryTrackerIndex()"
        :next-is-spelling="nextIsSpelling"
        :eager-fetch-count="eagerFetchCount ?? 5"
        @answered="onAnswered"
        @just-reviewed="onJustReviewed"
      />
      <AnsweredQuestionComponent
        v-if="currentAnsweredQuestion"
        v-bind="{ answeredQuestion: currentAnsweredQuestion, conversationButton: true }"
      />
      <AnsweredSpellingQuestion
        v-if="currentAnsweredSpelling"
        :answered-question="currentAnsweredSpelling"
      />
      <template v-else-if="toRepeatCount === 0 && previousAnsweredQuestionCursor === undefined">
        <div class="daisy-alert daisy-alert-success">
          You have finished all recalls for this half a day!
        </div>
        <div v-if="isLoadingMore" class="flex items-center gap-2 py-4">
          <span class="daisy-loading daisy-loading-spinner daisy-loading-md"></span>
          <span>Loading more items...</span>
        </div>
        <div v-else>
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
    </div>

  </div>
</template>

<script setup lang="ts">
import Quiz from "@/components/recall/Quiz.vue"
import RecallProgressBar from "@/components/recall/RecallProgressBar.vue"
import AnsweredQuestionComponent from "@/components/recall/AnsweredQuestionComponent.vue"
import AnsweredSpellingQuestion from "@/components/recall/AnsweredSpellingQuestion.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { RecallPrompt } from "@generated/doughnut-backend-api"
import {
  RecallsController,
  MemoryTrackerController,
} from "@generated/doughnut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import getEnvironment from "@/managedApi/window/getEnvironment"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { shuffle } from "es-toolkit"
import {
  computed,
  ref,
  onActivated,
  onDeactivated,
  onMounted,
  watch,
} from "vue"
import { useRecallData } from "@/composables/useRecallData"
import { useRecallTrackerNavigation } from "@/composables/useRecallTrackerNavigation"
import { useAssimilationCount } from "@/composables/useAssimilationCount"

const { popups } = usePopups()
const { dueCount, setDueCount } = useAssimilationCount()
const {
  currentRecallWindowEndAt,
  setCurrentRecallWindowEndAt,
  totalAssimilatedCount,
  setTotalAssimilatedCount,
  setIsRecallPaused,
  shouldResumeRecall,
  clearShouldResumeRecall,
  treadmillMode,
  setCurrentIndex,
  toRepeat,
  setToRepeat,
  diligentMode,
  setDiligentMode,
  dueRecallsRefreshNonce,
} = useRecallData()

defineProps({
  eagerFetchCount: Number,
})

const currentIndex = ref(0)
const previousAnsweredQuestions = ref<(RecallPrompt | undefined)[]>([])
const previousAnsweredQuestionCursor = ref<number | undefined>(undefined)
const isProgressBarVisible = ref(true)
const isLoadingMore = ref(false)

// Sync currentIndex with useRecallData
watch(
  () => currentIndex.value,
  (index) => {
    setCurrentIndex(index)
  },
  { immediate: true }
)

// Computed list of memory trackers that should not be modified
const memoryTrackers = computed(() => toRepeat.value ?? [])

const {
  nextIsSpelling,
  toRepeatCount,
  getCurrentMemoryTracker,
  getCurrentMemoryTrackerIndex,
  moveToNextMemoryTracker,
  moveMemoryTrackerToEnd,
  handleTreadmillModeChanged,
} = useRecallTrackerNavigation({
  toRepeat,
  currentIndex,
  treadmillMode,
  setToRepeat,
})

const currentAnsweredQuestion = computed(() => {
  if (previousAnsweredQuestionCursor.value === undefined) return undefined
  const result =
    previousAnsweredQuestions.value[previousAnsweredQuestionCursor.value]
  if (!result) return undefined
  return result?.questionType === "MCQ" ? result : undefined
})

const currentAnsweredSpelling = computed(() => {
  if (previousAnsweredQuestionCursor.value === undefined) return undefined
  const result =
    previousAnsweredQuestions.value[previousAnsweredQuestionCursor.value]
  if (!result) return undefined
  return result?.questionType === "SPELLING" ? result : undefined
})

const finished = computed(() => previousAnsweredQuestions.value.length)

const viewLastAnsweredQuestion = (cursor: number | undefined) => {
  previousAnsweredQuestionCursor.value = cursor
}

watch(
  () => previousAnsweredQuestionCursor.value,
  (cursor) => {
    setIsRecallPaused(cursor !== undefined)
  },
  { immediate: true }
)

watch(
  () => shouldResumeRecall.value,
  (shouldResume) => {
    if (shouldResume) {
      // Reset the cursor to show the current question instead of previously answered question
      previousAnsweredQuestionCursor.value = undefined
      clearShouldResumeRecall()
    }
  }
)

const loadMore = async (dueInDays?: number) => {
  isLoadingMore.value = true
  try {
    const { data: response, error } = await RecallsController.recalling({
      query: {
        timezone: timezoneParam(),
        dueindays: dueInDays,
      },
    })
    if (!error && response) {
      let trackers = response.toRepeat
      currentIndex.value = 0
      setTotalAssimilatedCount(response.totalAssimilatedCount)
      setDiligentMode((dueInDays ?? 0) > 0)
      if (trackers?.length === 0) {
        setToRepeat(trackers)
        return response
      }
      if (getEnvironment() !== "testing" && trackers) {
        trackers = shuffle(trackers)
      }
      setToRepeat(trackers)
      return response
    }
    return undefined
  } finally {
    isLoadingMore.value = false
  }
}

const offerReAssimilation = async (memoryTrackerId: number | undefined) => {
  if (!memoryTrackerId) return
  const confirmed = await popups.confirm(
    "You have answered this note incorrectly too many times. Would you like to re-assimilate it?"
  )
  if (confirmed) {
    await MemoryTrackerController.softDelete({
      path: { memoryTracker: memoryTrackerId },
    })
    setDueCount((dueCount.value ?? 0) + 1)
  }
}

const onAnswered = async (answerResult: RecallPrompt) => {
  moveToNextMemoryTracker()
  previousAnsweredQuestions.value.push(answerResult)
  if (!answerResult.answer?.correct) {
    viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
    const memoryTrackerId = answerResult.memoryTrackerId
    if (memoryTrackerId !== undefined) {
      const { data } = await apiCallWithLoading(() =>
        MemoryTrackerController.getThresholdExceeded({
          path: { memoryTracker: memoryTrackerId },
        })
      )
      if (data?.thresholdExceeded) {
        await offerReAssimilation(memoryTrackerId)
      }
    }
  }
}

const onJustReviewed = () => {
  moveToNextMemoryTracker()
  previousAnsweredQuestions.value.push(undefined)
}

const loadPreviouslyAnsweredRecallPrompts = async () => {
  const { data: response, error } = await RecallsController.previouslyAnswered({
    query: {
      timezone: timezoneParam(),
    },
  })
  if (!error && response) {
    previousAnsweredQuestions.value = [
      ...response,
      ...previousAnsweredQuestions.value,
    ]
  }
}

const loadCurrentDueRecalls = async () => {
  setToRepeat(undefined)
  const response = await loadMore(0)
  if (response) {
    setCurrentRecallWindowEndAt(response.currentRecallWindowEndAt)
  }
}

watch(dueRecallsRefreshNonce, async () => {
  await loadCurrentDueRecalls()
})

onMounted(() => {
  loadPreviouslyAnsweredRecallPrompts()
})

onActivated(() => {
  isProgressBarVisible.value = true
  const currentTime = new Date().toISOString()
  if (
    currentRecallWindowEndAt.value &&
    currentTime > currentRecallWindowEndAt.value
  ) {
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
:deep(.treadmill-mode) {
  background: linear-gradient(
    135deg,
    #667eea 0%,
    #764ba2 25%,
    #f093fb 50%,
    #4facfe 75%,
    #00f2fe 100%
  );
}
</style>

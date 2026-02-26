<template>
  <div class="recall-page daisy-h-full daisy-flex daisy-flex-col">
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

    <div class="daisy-flex-1 daisy-min-h-0 daisy-overflow-y-auto">
    <template v-if="toRepeat != undefined">
      <Quiz
        v-if="toRepeatCount !== 0 && getCurrentMemoryTracker() && (!treadmillMode || !getCurrentMemoryTracker()?.spelling)"
        v-show="!currentAnsweredQuestion && !currentAnsweredSpelling"
        :memory-trackers="memoryTrackers"
        :current-index="getCurrentMemoryTrackerIndex()"
        :eager-fetch-count="eagerFetchCount ?? 5"
        @answered-question="onAnsweredQuestion"
        @answered-spelling="onAnsweredSpelling"
        @just-reviewed="onJustReviewed"
      />
      <AnsweredQuestionComponent
        v-if="currentAnsweredQuestion"
        v-bind="{ answeredQuestion: currentAnsweredQuestion, conversationButton: true }"
      />
      <AnsweredSpellingQuestion
        v-if="currentAnsweredSpelling"
        v-bind="{ result: currentAnsweredSpelling }"
      />
      <template v-else-if="toRepeatCount === 0 && previousAnsweredQuestionCursor === undefined">
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
    </div>

  </div>
</template>

<script setup lang="ts">
import Quiz from "@/components/recall/Quiz.vue"
import RecallProgressBar from "@/components/recall/RecallProgressBar.vue"
import AnsweredQuestionComponent from "@/components/recall/AnsweredQuestionComponent.vue"
import AnsweredSpellingQuestion from "@/components/recall/AnsweredSpellingQuestion.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type {
  QuestionResult,
  SpellingResult,
  MemoryTrackerLite,
} from "@generated/backend"
import {
  RecallsController,
  MemoryTrackerController,
} from "@generated/backend/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import {} from "@/managedApi/clientSetup"
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
import { useAssimilationCount } from "@/composables/useAssimilationCount"

type RecallResult = QuestionResult | SpellingResult
const { popups } = usePopups()
const { dueCount, setDueCount } = useAssimilationCount()
const {
  currentRecallWindowEndAt,
  setCurrentRecallWindowEndAt,
  totalAssimilatedCount,
  setIsRecallPaused,
  shouldResumeRecall,
  clearShouldResumeRecall,
  treadmillMode,
  setCurrentIndex,
  toRepeat,
  setToRepeat,
  diligentMode,
  setDiligentMode,
} = useRecallData()

defineProps({
  eagerFetchCount: Number,
})

const currentIndex = ref(0)
const previousAnsweredQuestions = ref<(RecallResult | undefined)[]>([])
const previousAnsweredQuestionCursor = ref<number | undefined>(undefined)
const isProgressBarVisible = ref(true)

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

const getCurrentMemoryTracker = (): MemoryTrackerLite | undefined => {
  if (!toRepeat.value) return undefined
  if (!treadmillMode.value) {
    return toRepeat.value[currentIndex.value]
  }
  // In treadmill mode, skip spelling trackers
  for (let i = currentIndex.value; i < toRepeat.value.length; i++) {
    if (!toRepeat.value[i]?.spelling) {
      return toRepeat.value[i]
    }
  }
  return undefined
}

const getCurrentMemoryTrackerIndex = (): number => {
  if (!toRepeat.value) return 0
  if (!treadmillMode.value) return currentIndex.value
  // In treadmill mode, ensure we're pointing to a non-spelling tracker
  // If current index points to spelling, find next non-spelling
  let index = currentIndex.value
  while (index < toRepeat.value.length && toRepeat.value[index]?.spelling) {
    index++
  }
  return index < toRepeat.value.length ? index : currentIndex.value
}

const currentAnsweredQuestion = computed(() => {
  if (previousAnsweredQuestionCursor.value === undefined) return undefined
  const result =
    previousAnsweredQuestions.value[previousAnsweredQuestionCursor.value]
  if (!result) return undefined
  return result.type === "QuestionResult" ? result.answeredQuestion : undefined
})

const currentAnsweredSpelling = computed(() => {
  if (previousAnsweredQuestionCursor.value === undefined) return undefined
  const result =
    previousAnsweredQuestions.value[previousAnsweredQuestionCursor.value]
  if (!result) return undefined
  return result.type === "SpellingResult" ? result : undefined
})

const finished = computed(() => previousAnsweredQuestions.value.length)

const getRemainingNonSpellingCount = (): number => {
  if (!toRepeat.value) return 0
  if (!treadmillMode.value) {
    return toRepeat.value.length - currentIndex.value
  }
  // Count non-spelling trackers from current index onwards
  let count = 0
  for (let i = currentIndex.value; i < toRepeat.value.length; i++) {
    if (!toRepeat.value[i]?.spelling) {
      count++
    }
  }
  return count
}

const toRepeatCount = computed(() => getRemainingNonSpellingCount())

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
  () => treadmillMode.value,
  () => {
    if (toRepeat.value) {
      // Move to first non-spelling tracker if current is spelling and treadmill mode is on
      if (treadmillMode.value) {
        const currentTracker = toRepeat.value[currentIndex.value]
        if (currentTracker?.spelling) {
          const firstNonSpelling = toRepeat.value.findIndex(
            (t, idx) => !t.spelling && idx >= currentIndex.value
          )
          if (firstNonSpelling !== -1) {
            currentIndex.value = firstNonSpelling
          }
        }
      }
    }
  }
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
  const { data: response, error } = await RecallsController.recalling({
    query: {
      timezone: timezoneParam(),
      dueindays: dueInDays,
    },
  })
  if (!error && response) {
    let trackers = response.toRepeat
    currentIndex.value = 0
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
}

const moveToNextMemoryTracker = () => {
  if (!toRepeat.value) return
  if (!treadmillMode.value) {
    currentIndex.value += 1
    return
  }
  // Skip spelling memory trackers in treadmill mode
  let nextIndex = currentIndex.value + 1
  while (
    nextIndex < toRepeat.value.length &&
    toRepeat.value[nextIndex]?.spelling
  ) {
    nextIndex += 1
  }
  currentIndex.value = nextIndex
}

const offerReAssimilation = async (memoryTrackerId: number | undefined) => {
  if (!memoryTrackerId) return
  const confirmed = await popups.confirm(
    "You have answered this note incorrectly too many times. Would you like to re-assimilate it?"
  )
  if (confirmed) {
    await MemoryTrackerController.reAssimilate({
      path: { memoryTracker: memoryTrackerId },
    })
    setDueCount((dueCount.value ?? 0) + 1)
  }
}

const onAnsweredQuestion = async (answerResult: QuestionResult) => {
  moveToNextMemoryTracker()
  previousAnsweredQuestions.value.push(answerResult)
  if (!answerResult.answeredQuestion?.answer.correct) {
    viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
  }
  if (answerResult.answeredQuestion?.thresholdExceeded) {
    await offerReAssimilation(answerResult.answeredQuestion?.memoryTrackerId)
  }
}

const onAnsweredSpelling = async (answerResult: SpellingResult) => {
  moveToNextMemoryTracker()
  previousAnsweredQuestions.value.push(answerResult)
  if (!answerResult.isCorrect) {
    viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
  }
  if (answerResult.thresholdExceeded) {
    await offerReAssimilation(answerResult.memoryTrackerId)
  }
}

const onJustReviewed = () => {
  moveToNextMemoryTracker()
  previousAnsweredQuestions.value.push(undefined)
}

const moveMemoryTrackerToEnd = (index: number) => {
  const currentToRepeat = toRepeat.value
  if (!currentToRepeat) return

  const item = currentToRepeat[index]
  if (item === undefined) return
  setToRepeat([
    ...currentToRepeat.slice(0, index),
    ...currentToRepeat.slice(index + 1),
    item,
  ])
}

const handleTreadmillModeChanged = () => {
  // Adjust current index based on treadmill mode, but don't reset to 0
  // This prevents answered questions from being added back to the list
  if (toRepeat.value) {
    if (treadmillMode.value) {
      // In treadmill mode, move to first non-spelling tracker from current position
      const currentTracker = toRepeat.value[currentIndex.value]
      if (currentTracker?.spelling) {
        const firstNonSpelling = toRepeat.value.findIndex(
          (t, idx) => !t.spelling && idx >= currentIndex.value
        )
        if (firstNonSpelling !== -1) {
          currentIndex.value = firstNonSpelling
        }
      }
    } else {
      // When treadmill mode is turned off, move unanswered spelling trackers to the end
      const unansweredSpellingTrackers: MemoryTrackerLite[] = []
      const nonSpellingTrackers: MemoryTrackerLite[] = []

      // Separate spelling and non-spelling trackers from currentIndex onwards
      for (let i = currentIndex.value; i < toRepeat.value.length; i++) {
        const tracker = toRepeat.value[i]
        if (!tracker) continue
        if (tracker.spelling) {
          unansweredSpellingTrackers.push(tracker)
        } else {
          nonSpellingTrackers.push(tracker)
        }
      }

      // Rebuild the list: answered trackers (before currentIndex) + non-spelling trackers + spelling trackers
      if (unansweredSpellingTrackers.length > 0) {
        setToRepeat([
          ...toRepeat.value.slice(0, currentIndex.value),
          ...nonSpellingTrackers,
          ...unansweredSpellingTrackers,
        ])
      }
    }
  }
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

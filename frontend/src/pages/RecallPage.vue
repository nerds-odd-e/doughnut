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
        }"
        @view-last-answered-question="viewLastAnsweredQuestion($event)"
        @show-more="showTooltip = true"
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
  </div>
</template>

<script setup lang="ts">
import Quiz from "@/components/recall/Quiz.vue"
import RecallProgressBar from "@/components/recall/RecallProgressBar.vue"
import AnsweredQuestionComponent from "@/components/recall/AnsweredQuestionComponent.vue"
import AnsweredSpellingQuestion from "@/components/recall/AnsweredSpellingQuestion.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { AnsweredQuestion, SpellingResultDto } from "@generated/backend"
import type { MemoryTrackerLite } from "@generated/backend"
import { RecallsController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import getEnvironment from "@/managedApi/window/getEnvironment"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { shuffle } from "es-toolkit"
import {
  computed,
  onMounted,
  ref,
  onActivated,
  onDeactivated,
  watch,
} from "vue"
import { useRecallData } from "@/composables/useRecallData"

export type SpellingResult = SpellingResultDto & { type: "spelling" }

export type QuestionResult = {
  type: "question"
  answeredQuestion: AnsweredQuestion
}

type RecallResult = QuestionResult | SpellingResult
const {
  setToRepeatCount,
  decrementToRepeatCount,
  recallWindowEndAt,
  setRecallWindowEndAt,
  totalAssimilatedCount,
  setIsRecallPaused,
  shouldResumeRecall,
  clearShouldResumeRecall,
  treadmillMode,
  setCurrentRecallIndex,
} = useRecallData()

defineProps({
  eagerFetchCount: Number,
})

const toRepeat = ref<MemoryTrackerLite[] | undefined>(undefined)
const currentIndex = ref(0)
const previousAnsweredQuestions = ref<(RecallResult | undefined)[]>([])
const previousAnsweredQuestionCursor = ref<number | undefined>(undefined)
const isProgressBarVisible = ref(true)
const showTooltip = ref(false)

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
  return result.type === "question" ? result.answeredQuestion : undefined
})

const currentAnsweredSpelling = computed(() => {
  if (previousAnsweredQuestionCursor.value === undefined) return undefined
  const result =
    previousAnsweredQuestions.value[previousAnsweredQuestionCursor.value]
  if (!result) return undefined
  return result.type === "spelling" ? result : undefined
})

const finished = computed(() => previousAnsweredQuestions.value.length)

const getNonSpellingCount = (list: MemoryTrackerLite[] | undefined): number => {
  if (!list) return 0
  if (!treadmillMode.value) return list.length
  return list.filter((t) => !t.spelling).length
}

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

// Keep global recall index in sync for menu logic
watch(
  () => currentIndex.value,
  (idx) => {
    setCurrentRecallIndex(idx)
  },
  { immediate: true }
)

watch(
  () => treadmillMode.value,
  () => {
    if (toRepeat.value) {
      updateToRepeatCount()
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
    toRepeat.value = response.toRepeat
    currentIndex.value = 0
    setCurrentRecallIndex(0)
    if (toRepeat.value?.length === 0) {
      return response
    }
    if (getEnvironment() !== "testing" && toRepeat.value) {
      toRepeat.value = shuffle(toRepeat.value)
    }
    updateToRepeatCount(response.toRepeatCount)
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

const onAnsweredQuestion = (answerResult: AnsweredQuestion) => {
  moveToNextMemoryTracker()
  previousAnsweredQuestions.value.push({
    type: "question",
    answeredQuestion: answerResult,
  })
  if (!answerResult.answer.correct) {
    viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
  }
  decrementToRepeatCount()
}

const onAnsweredSpelling = (answerResult: SpellingResultDto) => {
  moveToNextMemoryTracker()
  previousAnsweredQuestions.value.push({
    type: "spelling",
    ...answerResult,
  })
  if (!answerResult.isCorrect) {
    viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
  }
  decrementToRepeatCount()
}

const onJustReviewed = () => {
  moveToNextMemoryTracker()
  previousAnsweredQuestions.value.push(undefined)
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
    }
    updateToRepeatCount()
  }
}

const updateToRepeatCount = (backendCount?: number) => {
  if (!toRepeat.value) {
    setToRepeatCount(0)
    return
  }
  if (treadmillMode.value) {
    const count = getNonSpellingCount(toRepeat.value)
    setToRepeatCount(count)
  } else if (backendCount !== undefined) {
    // Preserve backend count when treadmill mode is off
    setToRepeatCount(backendCount)
  } else {
    // Fallback to counting all trackers
    setToRepeatCount(toRepeat.value.length)
  }
}

const loadCurrentDueRecalls = async () => {
  toRepeat.value = undefined
  const response = await loadMore(0)
  if (response) {
    setRecallWindowEndAt(response.recallWindowEndAt)
  }
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
.repeat-paused {
  background-color: rgba(50, 150, 50, 0.8);
}

:deep(.treadmill-mode) {
  background: linear-gradient(
    135deg,
    #667eea 0%,
    #764ba2 25%,
    #f093fb 50%,
    #4facfe 75%,
    #00f2fe 100%
  );
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}
</style>

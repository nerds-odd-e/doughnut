<template>
  <div ref="recallPageRoot" class="recall-page h-full flex flex-col">
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
          potentialLearningSessions,
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
        :spelling-retry-nonce="spellingRetryNonce"
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
        @retry="onOverlapRetry"
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
import type { AnsweredQuestion } from "@generated/donut-backend-api"
import { computed, ref, watch } from "vue"
import { useRecallData } from "@/composables/useRecallData"
import { useRecallTrackerNavigation } from "@/composables/useRecallTrackerNavigation"
import { useRecallAnswerHandling } from "@/composables/useRecallAnswerHandling"
import { useRecallPageLoading } from "@/composables/useRecallPageLoading"
import { scheduleFocusAutofocusTargetWithin } from "@/utils/focusTarget"

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
  setDueCommissioned,
  potentialLearningSessions,
  diligentMode,
  setDiligentMode,
  dueRecallsRefreshNonce,
} = useRecallData()

defineProps({
  eagerFetchCount: Number,
})

const currentIndex = ref(0)
const previousAnsweredQuestions = ref<(AnsweredQuestion | undefined)[]>([])
const previousAnsweredQuestionCursor = ref<number | undefined>(undefined)
const spellingRetryNonce = ref(0)
const recallPageRoot = ref<HTMLElement | null>(null)

watch(
  () => currentIndex.value,
  (index) => {
    setCurrentIndex(index)
  },
  { immediate: true }
)

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

const { onAnswered, onOverlapRetry, onJustReviewed } = useRecallAnswerHandling({
  previousAnsweredQuestions,
  previousAnsweredQuestionCursor,
  spellingRetryNonce,
  moveToNextMemoryTracker,
  viewLastAnsweredQuestion,
})

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
      previousAnsweredQuestionCursor.value = undefined
      clearShouldResumeRecall()
      scheduleFocusAutofocusTargetWithin(recallPageRoot.value)
    }
  }
)

const { isProgressBarVisible, isLoadingMore, loadMore } = useRecallPageLoading({
  currentIndex,
  previousAnsweredQuestions,
  currentRecallWindowEndAt,
  dueRecallsRefreshNonce,
  setToRepeat,
  setDueCommissioned,
  setTotalAssimilatedCount,
  setDiligentMode,
  setCurrentRecallWindowEndAt,
})

defineExpose({
  toRepeat,
  currentIndex,
  loadMore,
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

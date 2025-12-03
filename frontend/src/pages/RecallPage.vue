<template>
  <GlobalBar
    v-if="isProgressBarVisible"
    :class="previousAnsweredQuestionCursor !== undefined ? 'repeat-paused' : ''"
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
    >
    </RecallProgressBar>
  </GlobalBar>

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
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { AnsweredQuestion, SpellingResultDto } from "@generated/backend"
import type { MemoryTrackerLite } from "@generated/backend"
import { RecallsController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import getEnvironment from "@/managedApi/window/getEnvironment"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { shuffle } from "es-toolkit"
import { computed, onMounted, ref, onActivated, onDeactivated } from "vue"
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
const toRepeatCount = computed(
  () => (toRepeat.value?.length ?? 0) - currentIndex.value
)

const viewLastAnsweredQuestion = (cursor: number | undefined) => {
  previousAnsweredQuestionCursor.value = cursor
}

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

const onAnsweredQuestion = (answerResult: AnsweredQuestion) => {
  currentIndex.value += 1
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
  currentIndex.value += 1
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
  currentIndex.value += 1
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
</style>

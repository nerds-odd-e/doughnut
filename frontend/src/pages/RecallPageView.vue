<template>
  <RecallProgressBar
    v-if="isProgressBarVisible"
    v-bind="{
      finished,
      toRepeatCount,
      previousAnsweredQuestionCursor,
    }"
    @view-last-answered-question="viewLastAnsweredQuestion($event)"
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

  <template>
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
        <button role="button" class="daisy-btn daisy-btn-secondary" @click="$emit('load-more', 3)">
          Load more from next 3 days
        </button>
        <button role="button" class="daisy-btn daisy-btn-secondary" @click="$emit('load-more', 7)">
          Load more from next 7 days
        </button>
        <button role="button" class="daisy-btn daisy-btn-secondary" @click="$emit('load-more', 14)">
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
import type { AnsweredQuestion, SpellingResultDto } from "@generated/backend"
import type { MemoryTrackerLite } from "@generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { computed, ref, watch } from "vue"
import { useRecallData } from "@/composables/useRecallData"

export type SpellingResult = SpellingResultDto & { type: "spelling" }

export type QuestionResult = {
  type: "question"
  answeredQuestion: AnsweredQuestion
}

type RecallResult = QuestionResult | SpellingResult

const { decrementToRepeatCount } = useRecallData()

const props = defineProps({
  toRepeat: {
    type: Array as PropType<MemoryTrackerLite[]>,
    required: true,
  },
  currentIndex: {
    type: Number,
    required: true,
  },
  totalAssimilatedCount: {
    type: Number,
    required: true,
  },
  eagerFetchCount: Number,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "answered-question", answerResult: AnsweredQuestion): void
  (e: "answered-spelling", answerResult: SpellingResultDto): void
  (e: "just-reviewed"): void
  (e: "move-to-end", index: number): void
  (e: "load-more", dueInDays: number): void
}>()

const previousAnsweredQuestions = ref<(RecallResult | undefined)[]>([])
const previousAnsweredQuestionCursor = ref<number | undefined>(undefined)
const isProgressBarVisible = ref(true)
const showTooltip = ref(false)

// Reset previousAnsweredQuestions when toRepeat changes (new session)
watch(
  () => props.toRepeat,
  () => {
    if (props.currentIndex === 0) {
      previousAnsweredQuestions.value = []
      previousAnsweredQuestionCursor.value = undefined
    }
  }
)

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
const toRepeatCount = computed(() => props.toRepeat.length - props.currentIndex)

const viewLastAnsweredQuestion = (cursor: number | undefined) => {
  previousAnsweredQuestionCursor.value = cursor
}

const onAnsweredQuestion = (answerResult: AnsweredQuestion) => {
  previousAnsweredQuestions.value.push({
    type: "question",
    answeredQuestion: answerResult,
  })
  if (!answerResult.answer.correct) {
    viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
  }
  decrementToRepeatCount()
  emit("answered-question", answerResult)
}

const onAnsweredSpelling = (answerResult: SpellingResultDto) => {
  previousAnsweredQuestions.value.push({
    type: "spelling",
    ...answerResult,
  })
  if (!answerResult.isCorrect) {
    viewLastAnsweredQuestion(previousAnsweredQuestions.value.length - 1)
  }
  decrementToRepeatCount()
  emit("answered-spelling", answerResult)
}

const onJustReviewed = () => {
  previousAnsweredQuestions.value.push(undefined)
  decrementToRepeatCount()
  emit("just-reviewed")
}

const moveMemoryTrackerToEnd = (index: number) => {
  emit("move-to-end", index)
}

defineExpose({
  toRepeat: computed(() => props.toRepeat),
  currentIndex: computed(() => props.currentIndex),
})
</script>


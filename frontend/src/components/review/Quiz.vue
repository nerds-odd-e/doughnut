<template>
  <div class="content">
    <ContentLoader v-if="!currentQuestionFetched || isCurrentMemoryTrackerFetching" />
    <template v-else>
      <SpellingQuestionComponent
        v-if="currentMemoryTracker?.spelling"
        v-bind="{
          memoryTrackerId: currentMemoryTrackerId!,
        }"
        @answer="onSpellingAnswer($event)"
        :key="`spelling-${currentMemoryTrackerId}`"
      />
      <template v-else>
        <div v-if="!currentRecallPrompt">
          <JustReview
            v-bind="{
              memoryTrackerId: currentMemoryTrackerId,
            }"
            @reviewed="() => emit('just-reviewed', undefined)"
          />
        </div>
        <template v-else>
         <div v-if="currentRecallPrompt.notebook" class="notebook-source daisy-mb-4">
            <NotebookLink :notebook="currentRecallPrompt.notebook" />
          </div>
          <ContestableQuestion
            v-bind="{
              recallPrompt: currentRecallPrompt,
            }"
            @answered="onAnswered($event)"
            :key="currentRecallPrompt.id"
          />
        </template>
      </template>
    </template>
    <button
      v-if="canMoveToEnd"
      class="daisy-btn daisy-btn-ghost daisy-btn-circle"
      title="Move to end of list"
      @click="moveToEnd"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <line x1="12" y1="5" x2="12" y2="19"></line>
        <polyline points="19 12 12 19 5 12"></polyline>
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type {
  AnsweredQuestion,
  RecallPrompt,
  AnswerSpellingDto,
  SpellingResultDto,
  MemoryTrackerLite,
} from "@generated/backend"
import {
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import ContestableQuestion from "./ContestableQuestion.vue"
import JustReview from "./JustReview.vue"
import SpellingQuestionComponent from "./SpellingQuestionComponent.vue"
import NotebookLink from "../notes/NotebookLink.vue"

// Interface definitions for better type safety
interface QuizProps {
  memoryTrackers: MemoryTrackerLite[]
  currentIndex: number
  eagerFetchCount: number
}

const props = defineProps<QuizProps>()

// Emits definition
const emit = defineEmits<{
  (e: "answered-question", result: AnsweredQuestion): void
  (e: "answered-spelling", result: SpellingResultDto): void
  (e: "just-reviewed", result: AnsweredQuestion | undefined): void
  (e: "moveToEnd", currentIndex: number): void
}>()

// Composable for question fetching logic
const useQuestionFetching = (props: QuizProps) => {
  const recallPromptCache = ref<Record<number, RecallPrompt | undefined>>({})
  const eagerFetchUntil = ref(0)
  const fetching = ref(false)
  const fetchingMemoryTrackerIds = ref<Set<number>>(new Set())

  const fetchNextQuestion = async () => {
    for (
      let index = props.currentIndex;
      index < props.currentIndex + props.eagerFetchCount;
      index++
    ) {
      const memoryTracker = memoryTrackerAt(index)
      const memoryTrackerId = memoryTracker?.memoryTrackerId
      if (memoryTrackerId === undefined) break

      const cachedValue = recallPromptCache.value[memoryTrackerId]
      if (cachedValue !== undefined) continue

      fetchingMemoryTrackerIds.value.add(memoryTrackerId)
      try {
        const { data: question, error } =
          await RecallPromptController.askAQuestion({
            path: { memoryTracker: memoryTrackerId },
          })
        if (!error) {
          recallPromptCache.value[memoryTrackerId] = question!
        } else {
          recallPromptCache.value[memoryTrackerId] = undefined
        }
      } finally {
        fetchingMemoryTrackerIds.value.delete(memoryTrackerId)
      }
    }
  }

  const fetchQuestion = async () => {
    eagerFetchUntil.value = props.currentIndex + props.eagerFetchCount

    if (!fetching.value) {
      fetching.value = true
      try {
        await fetchNextQuestion()
      } finally {
        fetching.value = false
      }
    }
  }

  return {
    recallPromptCache,
    fetchQuestion,
    fetchingMemoryTrackerIds,
  }
}

// Use the composable
const { recallPromptCache, fetchQuestion, fetchingMemoryTrackerIds } =
  useQuestionFetching(props)

// Computed properties with better naming
const currentMemoryTracker = computed(() => memoryTrackerAt(props.currentIndex))
const currentMemoryTrackerId = computed(
  () => currentMemoryTracker.value?.memoryTrackerId
)
const isCurrentMemoryTrackerFetching = computed(() => {
  const memoryTrackerId = currentMemoryTrackerId.value
  return (
    memoryTrackerId !== undefined &&
    fetchingMemoryTrackerIds.value.has(memoryTrackerId)
  )
})
const currentQuestionFetched = computed(() => {
  const memoryTrackerId = currentMemoryTrackerId.value
  return (
    memoryTrackerId !== undefined && memoryTrackerId in recallPromptCache.value
  )
})
const currentRecallPrompt = computed(() => {
  const memoryTrackerId = currentMemoryTrackerId.value
  return memoryTrackerId !== undefined
    ? recallPromptCache.value[memoryTrackerId]
    : undefined
})

// Methods
const memoryTrackerAt = (index: number): MemoryTrackerLite | undefined => {
  return props.memoryTrackers?.[index]
}

const onSpellingAnswer = async (answerData: AnswerSpellingDto) => {
  if (answerData.spellingAnswer === undefined || !currentMemoryTrackerId.value)
    return

  const { data: answerResult, error } = await apiCallWithLoading(() =>
    MemoryTrackerController.answerSpelling({
      path: { memoryTracker: currentMemoryTrackerId.value! },
      body: { spellingAnswer: answerData.spellingAnswer! },
    })
  )
  if (!error) {
    emit("answered-spelling", answerResult!)
  }
}

const onAnswered = (answerResult: AnsweredQuestion) => {
  emit("answered-question", answerResult)
}

const canMoveToEnd = computed(() => {
  return props.currentIndex < (props.memoryTrackers?.length ?? 0) - 1
})

const moveToEnd = () => {
  // Pre-mark the next question (which will become the new current question
  // after moving current item to end) as fetched (even if undefined) to prevent
  // showing the loader while fetching. This must happen before the emit to
  // ensure the cache is updated before the watcher fires.
  const nextIndex = props.currentIndex + 1
  const nextMemoryTracker = memoryTrackerAt(nextIndex)
  const nextMemoryTrackerId = nextMemoryTracker?.memoryTrackerId
  if (
    nextMemoryTrackerId !== undefined &&
    !(nextMemoryTrackerId in recallPromptCache.value)
  ) {
    recallPromptCache.value[nextMemoryTrackerId] = undefined
  }
  emit("moveToEnd", props.currentIndex)
}

// Watchers
watch(() => currentMemoryTrackerId.value, fetchQuestion)

// Lifecycle hooks
onMounted(() => {
  fetchQuestion()
})
</script>

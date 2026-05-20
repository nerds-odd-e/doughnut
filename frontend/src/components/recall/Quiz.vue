<template>
  <div
    class="content daisy-h-full"
    :class="{ 'quiz--contestable': showContestableDummyInput }"
  >
    <ContentLoader v-if="!currentQuestionFetched || isCurrentMemoryTrackerFetching" />
    <template v-else>
      <div class="pt-5 h-full">
      <SpellingQuestionDisplay
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
         <div class="notebook-source mb-4">
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
      </div>
    </template>
    <div
      v-if="showContestableDummyInput"
      class="contestable-dummy-input-bar"
      aria-hidden="true"
    >
      <input
        type="text"
        class="daisy-input daisy-input-bordered daisy-w-full"
        readonly
        tabindex="-1"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type {
  AnswerSpellingDto,
  MemoryTrackerLite,
  RecallPrompt,
} from "@generated/doughnut-backend-api"
import {
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import ContestableQuestion from "./ContestableQuestion.vue"
import JustReview from "./JustReview.vue"
import SpellingQuestionDisplay from "./SpellingQuestionDisplay.vue"
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
  (e: "answered", result: RecallPrompt): void
  (e: "just-reviewed", result: RecallPrompt | undefined): void
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
      if (memoryTracker === undefined) break

      const memoryTrackerId = memoryTracker.memoryTrackerId

      // All memory trackers (including spelling) can use askAQuestion

      const cachedValue = recallPromptCache.value[memoryTrackerId]
      if (cachedValue !== undefined) continue

      fetchingMemoryTrackerIds.value.add(memoryTrackerId)
      try {
        const { data: question, error } = await apiCallWithLoading(() =>
          MemoryTrackerController.askAQuestion({
            path: { memoryTracker: memoryTrackerId },
          })
        )
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
const showContestableDummyInput = computed(
  () =>
    currentQuestionFetched.value &&
    !isCurrentMemoryTrackerFetching.value &&
    !currentMemoryTracker.value?.spelling &&
    currentRecallPrompt.value !== undefined
)

// Methods
const memoryTrackerAt = (index: number): MemoryTrackerLite | undefined =>
  props.memoryTrackers?.[index]

const onSpellingAnswer = async (
  answerData: AnswerSpellingDto & { recallPromptId?: number }
) => {
  if (answerData.spellingAnswer === undefined || !answerData.recallPromptId)
    return

  const { data: answerResult, error } = await apiCallWithLoading(() =>
    RecallPromptController.answerSpelling({
      path: { recallPrompt: answerData.recallPromptId! },
      body: {
        spellingAnswer: answerData.spellingAnswer!,
        thinkingTimeMs: answerData.thinkingTimeMs,
      },
    })
  )
  if (!error) {
    emit("answered", answerResult!)
  }
}

const onAnswered = (answerResult: RecallPrompt) => {
  emit("answered", answerResult)
}

// Watchers
watch(() => currentMemoryTrackerId.value, fetchQuestion)

// Lifecycle hooks
onMounted(() => {
  fetchQuestion()
})
</script>

<style scoped lang="scss">
@use "@/assets/menu-variables.scss" as *;

$contestable-dummy-input-reserve: calc(
  0.5rem + 3rem + max(0.75rem, env(safe-area-inset-bottom))
);

.quiz--contestable {
  padding-bottom: $contestable-dummy-input-reserve;
}

.contestable-dummy-input-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: $main-menu-width;
  z-index: 40;
  padding: 0.5rem 1rem max(0.75rem, env(safe-area-inset-bottom));
  background-color: hsl(var(--b1));
}

@media (max-width: theme("screens.lg")) {
  .contestable-dummy-input-bar {
    left: 0;
  }
}
</style>

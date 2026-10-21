<template>
  <div class="content h-full flex flex-col">
    <div
      v-if="currentMemoryTracker?.spelling && spellingOverlapFeedback"
      class="daisy-alert daisy-alert-warning mb-2 shrink-0"
      data-testid="spelling-overlap-feedback"
    >{{ spellingOverlapFeedback }}</div>
    <ContentLoader v-if="!currentRecallPromptLoad || currentRecallPromptLoad.status === 'loading'" />
    <div v-else class="pt-5 flex-1 min-h-0">
      <JustReview
        v-if="selfReviewTrackerIds.has(currentMemoryTrackerId!)"
        :memory-tracker-id="currentMemoryTrackerId"
        @reviewed="() => emit('just-reviewed', undefined)"
      />
      <div v-else-if="currentRecallPromptLoad.status === 'failed'">
        <p>We couldn't load this question.</p>
        <button
          class="daisy-btn daisy-btn-primary mt-4"
          data-testid="retry-question"
          @click="retryRecallPrompt(currentMemoryTrackerId!)"
        >Retry</button>
        <button
          v-if="!currentMemoryTracker?.spelling"
          class="daisy-btn daisy-btn-secondary mt-4"
          data-testid="self-review"
          @click="selfReviewTrackerIds.add(currentMemoryTrackerId!)"
        >Self-review</button>
      </div>
      <template v-else-if="currentRecallPrompt">
        <SpellingQuestionDisplay
          v-if="currentMemoryTracker?.spelling"
          :recall-prompt="currentRecallPrompt"
          :next-is-spelling="nextIsSpelling"
          @answered="onAnswered($event)"
          :key="currentRecallPrompt.id"
        />
        <template v-else>
          <div class="notebook-source mb-4">
            <NotebookLink :notebook="currentRecallPrompt.notebook" />
          </div>
          <ContestableQuestion
            :recall-prompt="currentRecallPrompt"
            :next-is-spelling="nextIsSpelling"
            @answered="onAnswered($event)"
            :key="currentRecallPrompt.id"
          />
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, onMounted, ref } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type {
  MemoryTrackerLite,
  AnsweredQuestion,
} from "@generated/donut-backend-api"
import ContestableQuestion from "./ContestableQuestion.vue"
import JustReview from "./JustReview.vue"
import SpellingQuestionDisplay from "./SpellingQuestionDisplay.vue"
import NotebookLink from "../notes/NotebookLink.vue"
import { useRecallPromptFetching } from "./useRecallPromptFetching"

// Interface definitions for better type safety
interface RecallPromptCardProps {
  memoryTrackers: MemoryTrackerLite[]
  currentIndex: number
  eagerFetchCount: number
  nextIsSpelling?: boolean
  spellingRetryNonce?: number
  spellingOverlapFeedback?: string
}

const props = defineProps<RecallPromptCardProps>()

// Emits definition
const emit = defineEmits<{
  (e: "answered", result: AnsweredQuestion): void
  (e: "just-reviewed", result: undefined): void
}>()

const {
  recallPromptLoads,
  fetchRecallPrompts,
  retryRecallPrompt,
  startRecallAttempt,
} = useRecallPromptFetching(props)
const selfReviewTrackerIds = ref(new Set<number>())

// Computed properties with better naming
const currentMemoryTracker = computed(() => memoryTrackerAt(props.currentIndex))
const currentMemoryTrackerId = computed(
  () => currentMemoryTracker.value?.memoryTrackerId
)
const currentRecallPromptLoad = computed(() => {
  const memoryTrackerId = currentMemoryTrackerId.value
  return memoryTrackerId !== undefined
    ? recallPromptLoads.value[memoryTrackerId]
    : undefined
})
const currentRecallPrompt = computed(() => {
  const load = currentRecallPromptLoad.value
  return load?.status === "loaded" ? load.recallPrompt : undefined
})

// Methods
const memoryTrackerAt = (index: number): MemoryTrackerLite | undefined =>
  props.memoryTrackers?.[index]

const onAnswered = (answerResult: AnsweredQuestion) => {
  emit("answered", answerResult)
}

// Watchers
watch(() => currentMemoryTrackerId.value, fetchRecallPrompts)

watch(
  () => props.spellingRetryNonce,
  () => {
    if (currentMemoryTracker.value?.spelling) {
      startRecallAttempt(currentMemoryTrackerId.value!)
    }
  }
)

// Lifecycle hooks
onMounted(() => {
  fetchRecallPrompts()
})
</script>

<template>
  <div class="content h-full">
    <ContentLoader v-if="!currentRecallPromptLoad || currentRecallPromptLoad.status === 'loading'" />
    <template v-else>
      <div class="pt-5 h-full">
      <SpellingQuestionDisplay
        v-if="currentMemoryTracker?.spelling"
        v-bind="{
          memoryTrackerId: currentMemoryTrackerId!,
          nextIsSpelling,
          overlapFeedback: spellingOverlapFeedback,
        }"
        @answered="onAnswered($event)"
        :key="`spelling-${currentMemoryTrackerId}-${props.spellingRetryNonce ?? 0}`"
      />
      <template v-else>
        <div v-if="selfReviewTrackerIds.has(currentMemoryTrackerId!)">
          <JustReview
            v-bind="{
              memoryTrackerId: currentMemoryTrackerId,
            }"
            @reviewed="() => emit('just-reviewed', undefined)"
          />
        </div>
        <div v-else-if="currentRecallPromptLoad.status === 'failed'">
          <p>We couldn't load this question.</p>
          <button
            class="daisy-btn daisy-btn-secondary mt-4"
            data-testid="self-review"
            @click="selfReviewTrackerIds.add(currentMemoryTrackerId!)"
          >Self-review</button>
        </div>
        <template v-else-if="currentRecallPrompt">
         <div class="notebook-source mb-4">
            <NotebookLink :notebook="currentRecallPrompt.notebook" />
          </div>
          <ContestableQuestion
            v-bind="{
              recallPrompt: currentRecallPrompt,
              nextIsSpelling,
            }"
            @answered="onAnswered($event)"
            :key="currentRecallPrompt.id"
          />
        </template>
      </template>
      </div>
    </template>
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

const { recallPromptLoads, fetchRecallPrompts } = useRecallPromptFetching(props)
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

// Lifecycle hooks
onMounted(() => {
  fetchRecallPrompts()
})
</script>

<template>
  <div class="content h-full">
    <ContentLoader v-if="!currentRecallPromptFetched || isCurrentMemoryTrackerFetching" />
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
import { computed, watch, onMounted } from "vue"
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

const { recallPromptCache, fetchRecallPrompts, fetchingMemoryTrackerIds } =
  useRecallPromptFetching(props)

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
const currentRecallPromptFetched = computed(() => {
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

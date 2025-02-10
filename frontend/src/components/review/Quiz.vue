<template>
  <div class="content">
    <ContentLoader v-if="!currentQuestionFetched" />
    <template v-else>
      <div v-if="!currentRecallPrompt">
        <JustReview
          v-bind="{
            memoryTrackerId: currentMemoryTrackerId,
            storageAccessor,
          }"
          @reviewed="onAnswered($event)"
        />
      </div>
      <ContestableQuestion
        v-else
        v-bind="{
          recallPrompt: currentRecallPrompt,
          storageAccessor,
        }"
        @answered="onAnswered($event)"
        :key="currentRecallPrompt.id"
      />
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
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type { AnsweredQuestion, RecallPrompt } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import ContestableQuestion from "./ContestableQuestion.vue"
import JustReview from "./JustReview.vue"

// Interface definitions for better type safety
interface QuizProps {
  memoryTrackers: number[]
  currentIndex: number
  eagerFetchCount: number
  storageAccessor: StorageAccessor
}

const props = defineProps<QuizProps>()

// Emits definition
const emit = defineEmits<{
  (e: "answered", result: AnsweredQuestion): void
  (e: "moveToEnd", currentIndex: number): void
}>()

// Composable for question fetching logic
const useQuestionFetching = (props: QuizProps) => {
  const recallPromptCache = ref<Record<number, RecallPrompt | undefined>>({})
  const eagerFetchUntil = ref(0)
  const fetching = ref(false)
  const { managedApi } = useLoadingApi()

  const fetchNextQuestion = async () => {
    for (
      let index = props.currentIndex;
      index < props.currentIndex + props.eagerFetchCount;
      index++
    ) {
      const memoryTrackerId = memoryTrackerIdAt(index)
      if (memoryTrackerId === undefined) break

      if (memoryTrackerId in recallPromptCache.value) continue

      try {
        const question =
          await managedApi.silent.restRecallPromptController.askAQuestion(
            memoryTrackerId
          )
        recallPromptCache.value[memoryTrackerId] = question
      } catch (e) {
        recallPromptCache.value[memoryTrackerId] = undefined
      }
    }
  }

  const fetchQuestion = async () => {
    eagerFetchUntil.value = props.currentIndex + props.eagerFetchCount

    if (!fetching.value) {
      fetching.value = true
      await fetchNextQuestion()
      fetching.value = false
    }
  }

  return {
    recallPromptCache,
    fetchQuestion,
  }
}

// Use the composable
const { recallPromptCache, fetchQuestion } = useQuestionFetching(props)

// Computed properties with better naming
const currentMemoryTrackerId = computed(() =>
  memoryTrackerIdAt(props.currentIndex)
)
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
const memoryTrackerIdAt = (index: number): number | undefined => {
  if (props.memoryTrackers && index < props.memoryTrackers.length) {
    return props.memoryTrackers[index]
  }
  return undefined
}

const onAnswered = (answerResult: AnsweredQuestion) => {
  emit("answered", answerResult)
}

const canMoveToEnd = computed(() => {
  return props.currentIndex < (props.memoryTrackers?.length ?? 0) - 1
})

const moveToEnd = () => {
  emit("moveToEnd", props.currentIndex)
}

// Watchers
watch(() => currentMemoryTrackerId.value, fetchQuestion)

// Lifecycle hooks
onMounted(() => {
  fetchQuestion()
})
</script>

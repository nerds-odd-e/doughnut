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
          @reviewed="(result) => emit('just-reviewed', result)"
        />
      </div>
      <template v-else>
        <div v-if="currentRecallPrompt.notebook" class="notebook-source daisy-mb-4">
          <NotebookLink :notebook="currentRecallPrompt.notebook" />
        </div>
        <SpellingQuestionComponent
          v-if="currentRecallPrompt && (!currentRecallPrompt.bareQuestion.multipleChoicesQuestion.choices || currentRecallPrompt.bareQuestion.multipleChoicesQuestion.choices.length === 0)"
          v-bind="{
            bareQuestion: currentRecallPrompt.bareQuestion,
            memoryTrackerId: currentMemoryTrackerId!,
          }"
          @answer="onSpellingAnswer($event)"
          :key="`spelling-${currentRecallPrompt.id}`"
        />
        <ContestableQuestion
          v-else
          v-bind="{
            recallPrompt: currentRecallPrompt,
            storageAccessor,
          }"
          @answered="onAnswered($event)"
          :key="currentRecallPrompt.id"
        />
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
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type {
  AnsweredQuestion,
  RecallPrompt,
  AnswerSpellingDTO,
  SpellingResultDTO,
  MemoryTrackerLite,
} from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import ContestableQuestion from "./ContestableQuestion.vue"
import JustReview from "./JustReview.vue"
import SpellingQuestionComponent from "./SpellingQuestionComponent.vue"
import NotebookLink from "../notes/NotebookLink.vue"

// Interface definitions for better type safety
interface QuizProps {
  memoryTrackers: MemoryTrackerLite[]
  currentIndex: number
  eagerFetchCount: number
  storageAccessor: StorageAccessor
}

const props = defineProps<QuizProps>()

// Emits definition
const emit = defineEmits<{
  (e: "answered-question", result: AnsweredQuestion): void
  (e: "answered-spelling", result: SpellingResultDTO): void
  (e: "just-reviewed", result: AnsweredQuestion | undefined): void
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
const { managedApi } = useLoadingApi()

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
  const tracker = props.memoryTrackers?.[index]
  return tracker?.memoryTrackerId
}

const onSpellingAnswer = async (answerData: AnswerSpellingDTO) => {
  if (answerData.spellingAnswer === undefined || !currentRecallPrompt.value)
    return

  try {
    const answerResult =
      await managedApi.restRecallPromptController.answerSpelling(
        currentRecallPrompt.value.id,
        { spellingAnswer: answerData.spellingAnswer }
      )
    emit("answered-spelling", answerResult)
  } catch (e) {
    // Error handling is already done in the component
  }
}

const onAnswered = (answerResult: AnsweredQuestion) => {
  emit("answered-question", answerResult)
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

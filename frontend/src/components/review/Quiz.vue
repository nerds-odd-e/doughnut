<template>
  <div v-if="!minimized" class="content">
    <ContentLoader v-if="!currentQuestionFetched" />
    <template v-else>
      <div v-if="!currentReviewQuestion">
        <JustReview
          v-bind="{
            reviewPointId: currentReviewPointId,
            storageAccessor,
          }"
          @reviewed="onAnswered($event)"
        />
      </div>
      <ContestableQuestion
        v-else
        v-bind="{
          reviewQuestionInstance: currentReviewQuestion,
          storageAccessor,
        }"
        @answered="onAnswered($event)"
        :key="currentReviewQuestion.id"
      />
      <button
        v-if="canMoveToEnd"
        class="btn btn-icon"
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
  ReviewQuestionInstance,
} from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import _ from "lodash"
import ContestableQuestion from "./ContestableQuestion.vue"
import JustReview from "./JustReview.vue"

// Interface definitions for better type safety
interface QuizProps {
  minimized?: boolean
  reviewPoints: number[]
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
  const reviewQuestionCache = ref<
    Record<number, ReviewQuestionInstance | undefined>
  >({})
  const eagerFetchUntil = ref(0)
  const fetching = ref(false)
  const { managedApi } = useLoadingApi()

  const fetchNextQuestion = async () => {
    for (
      let index = props.currentIndex;
      index < props.currentIndex + props.eagerFetchCount;
      index++
    ) {
      const reviewPointId = reviewPointIdAt(index)
      if (reviewPointId === undefined) break

      if (reviewPointId in reviewQuestionCache.value) continue

      try {
        const question =
          await managedApi.silent.restReviewQuestionController.generateRandomQuestion(
            reviewPointId
          )
        reviewQuestionCache.value[reviewPointId] = question
      } catch (e) {
        reviewQuestionCache.value[reviewPointId] = undefined
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
    reviewQuestionCache,
    fetchQuestion,
  }
}

// Use the composable
const { reviewQuestionCache, fetchQuestion } = useQuestionFetching(props)

// Computed properties with better naming
const currentReviewPointId = computed(() => reviewPointIdAt(props.currentIndex))
const currentQuestionFetched = computed(() => {
  const reviewPointId = currentReviewPointId.value
  return (
    reviewPointId !== undefined && reviewPointId in reviewQuestionCache.value
  )
})
const currentReviewQuestion = computed(() => {
  const reviewPointId = currentReviewPointId.value
  return reviewPointId !== undefined
    ? reviewQuestionCache.value[reviewPointId]
    : undefined
})

// Methods
const reviewPointIdAt = (index: number): number | undefined => {
  if (props.reviewPoints && index < props.reviewPoints.length) {
    return props.reviewPoints[index]
  }
  return undefined
}

const onAnswered = (answerResult: AnsweredQuestion) => {
  emit("answered", answerResult)
}

const canMoveToEnd = computed(() => {
  return props.currentIndex < (props.reviewPoints?.length ?? 0) - 1
})

const moveToEnd = () => {
  emit("moveToEnd", props.currentIndex)
}

// Watchers
watch(() => currentReviewPointId.value, fetchQuestion)

// Lifecycle hooks
onMounted(() => {
  fetchQuestion()
})
</script>

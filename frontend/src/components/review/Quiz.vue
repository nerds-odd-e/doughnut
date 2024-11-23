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
    const index = Object.keys(reviewQuestionCache.value).length
    if (eagerFetchUntil.value <= index) return

    const reviewPointId = reviewPointIdAt(index)
    if (reviewPointId === undefined) return

    try {
      const question =
        await managedApi.silent.restReviewQuestionController.generateRandomQuestion(
          reviewPointId
        )
      reviewQuestionCache.value[reviewPointId] = question
    } catch (e) {
      reviewQuestionCache.value[reviewPointId] = undefined
    }
    await fetchNextQuestion()
  }

  const fetchQuestion = async () => {
    eagerFetchUntil.value = _.max([
      eagerFetchUntil.value,
      props.currentIndex + props.eagerFetchCount,
    ]) as number

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

const selectPosition = () => {
  if (props.minimized) return
}

const onAnswered = (answerResult: AnsweredQuestion) => {
  emit("answered", answerResult)
}

// Watchers
watch(() => props.minimized, selectPosition)
watch(() => props.currentIndex, fetchQuestion)
watch(() => currentReviewQuestion.value, selectPosition)

// Lifecycle hooks
onMounted(() => {
  fetchQuestion()
})
</script>

<template>
  <RepeatProgressBar
    v-bind="{
      finished,
      toRepeatCount: remainingCount,
      previousResultCursor,
    }"
    @view-last-result="viewLastResult($event)"
  >
  </RepeatProgressBar>
  <template v-if="toRepeat != undefined">
    <Quiz
      v-if="remainingCount !== 0"
      :minimized="minimized"
      :memory-trackers="toRepeat"
      :current-index="currentIndex"
      :eager-fetch-count="eagerFetchCount ?? 5"
      :storage-accessor="storageAccessor"
      @answered="onAnswered($event)"
      @move-to-end="moveToEnd"
    />
    <template v-else-if="!minimized">
      <div class="alert alert-success">
        You have finished all repetitions for this half a day!
      </div>
      <div>
        <button role="button" class="btn btn-secondary" @click="loadMore(3)">
          Load more from next 3 days
        </button>
        <button role="button" class="btn btn-secondary" @click="loadMore(7)">
          Load more from next 7 days
        </button>
        <button role="button" class="btn btn-secondary" @click="loadMore(14)">
          Load more from next 14 days
        </button>
      </div>
    </template>
  </template>
</template>

<script setup lang="ts">
import Quiz from "@/components/review/Quiz.vue"
import RepeatProgressBar from "@/components/review/RepeatProgressBar.vue"
import type { AnsweredQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useRouter } from "vue-router"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { computed, onMounted, ref } from "vue"
import { useRecallData } from "@/composables/useRecallData"

const $router = useRouter()
const { managedApi } = useLoadingApi()
const {
  toRepeat,
  currentIndex,
  previousResults,
  decrementToRepeatCount,
  setToRepeat,
  addPreviousResult,
  moveToEnd,
  remainingCount,
} = useRecallData()

defineProps({
  minimized: Boolean,
  eagerFetchCount: Number,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const previousResultCursor = ref<number | undefined>(undefined)

const currentResult = computed(() => {
  if (previousResultCursor.value === undefined) return undefined
  return previousResults.value[previousResultCursor.value]
})

const finished = computed(() => previousResults.value.length)

const viewLastResult = (cursor: number | undefined) => {
  previousResultCursor.value = cursor
  if (currentResult.value) {
    const { recallPromptId } = currentResult.value
    $router.push({
      name: "repeat-answer",
      params: { recallPromptId },
    })
    return
  }
  $router.push({ name: "repeat" })
}

const loadMore = async (dueInDays?: number) => {
  const response = await managedApi.restRecallsController.repeatReview(
    timezoneParam(),
    dueInDays
  )
  await setToRepeat(response.toRepeat)
}

const onAnswered = (answerResult: AnsweredQuestion) => {
  addPreviousResult(answerResult)
  decrementToRepeatCount()
  if (!answerResult) return
  if (!answerResult.answer.correct) {
    viewLastResult(previousResults.value.length - 1)
  }
}

onMounted(() => {
  loadMore(0)
})

defineExpose({
  toRepeat,
  currentIndex,
})
</script>

<template>
  <RepeatProgressBar
    v-bind="{
      finished,
      toRepeatCount,
      previousResultCursor,
    }"
    @view-last-result="viewLastResult($event)"
  >
  </RepeatProgressBar>
  <template v-if="toRepeat != undefined">
    <Quiz
      v-if="toRepeatCount !== 0"
      :minimized="minimized"
      :review-points="toRepeat"
      :current-index="currentIndex"
      :eager-fetch-count="eagerFetchCount ?? 5"
      :storage-accessor="storageAccessor"
      @answered="onAnswered($event)"
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
import { AnsweredQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import getEnvironment from "@/managedApi/window/getEnvironment"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useRouter } from "vue-router"
import { StorageAccessor } from "@/store/createNoteStorage"
import _ from "lodash"
import { PropType, computed, onMounted, ref,  } from "vue"

const $router = useRouter()
const { managedApi }    = useLoadingApi()
  defineProps({
    minimized: Boolean,
    eagerFetchCount: Number,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  })

  const toRepeat = ref<number[] | undefined>(undefined)
  const currentIndex = ref(0)
  const previousResults = ref<(AnsweredQuestion | undefined)[]>([])
  const previousResultCursor = ref<number | undefined>(undefined)

  const currentResult = computed(() => {
    if (previousResultCursor.value === undefined) return undefined
    return previousResults.value[previousResultCursor.value]
  })

  const finished = computed(() => previousResults.value.length)
  const toRepeatCount = computed(() => (toRepeat.value?.length ?? 0) - currentIndex.value)

  const viewLastResult = (cursor: number | undefined) => {
    previousResultCursor.value = cursor
    if (currentResult.value) {
      const { answerId } = currentResult.value
      $router.push({ name: "repeat-answer", params: { answerId } })
      return
    }
    $router.push({ name: "repeat" })
  }

  const loadMore = async (dueInDays?: number) => {
    toRepeat.value = (
      await managedApi.restReviewsController.repeatReview(
        timezoneParam(),
        dueInDays,
      )
    ).toRepeat
    currentIndex.value = 0
    if (toRepeat.value?.length === 0) {
      return
    }
    if (getEnvironment() !== "testing") {
      toRepeat.value = _.shuffle(toRepeat.value)
    }
  }

  const onAnswered = (answerResult: AnsweredQuestion) => {
    currentIndex.value += 1
    previousResults.value.push(answerResult)
    if (!answerResult) return
    if (!answerResult.correct) {
      viewLastResult(previousResults.value.length - 1)
    }
  }

  onMounted(() => {
    loadMore(0)})


</script>

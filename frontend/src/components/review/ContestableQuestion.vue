<template>
  <BasicBreadcrumb
    v-if="reviewQuestionInstance.notebook"
    :ancestors="[reviewQuestionInstance.notebook.headNote.noteTopic]"
  />
  <div v-for="(q, index) in prevQuestions" :key="index">
    <h3>Previous Question Contested ...</h3>
    <p>{{ q.badQuestionReason }}</p>
    <ReviewQuestion :review-question-instance="q.quizeQuestion" :disabled="true" />
  </div>
  <p v-if="currentQuestionLegitMessage">{{ currentQuestionLegitMessage }}</p>
  <ContentLoader v-if="regenerating" />
  <div class="review-question-instance" v-else>
    <AnsweredQuestionComponent
      v-if="answeredQuestion"
      :answered-question="answeredQuestion"
      :storage-accessor="storageAccessor"
    />
    <div v-else>
    <ReviewQuestion
      :review-question-instance="currentQuestion"
      @answered="onAnswered($event)"
    />
      <a
        role="button"
        title="Doesn't make sense?"
        id="try-again"
        v-if="currentQuestion"
        class="btn"
        @click="contest"
      >
        <SvgContest />
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { AnsweredQuestion, ReviewQuestionInstance } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, ref } from "vue"
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue"
import AnsweredQuestionComponent from "./AnsweredQuestionComponent.vue"
import ReviewQuestion from "./ReviewQuestion.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  reviewQuestionInstance: {
    type: Object as PropType<ReviewQuestionInstance>,
    required: true,
  },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
const emit = defineEmits(["need-scroll", "answered"])
const regenerating = ref(false)
const currentQuestionLegitMessage = ref<string | undefined>(undefined)
const currentQuestion = ref(props.reviewQuestionInstance)
const answeredQuestion = ref<AnsweredQuestion | undefined>(undefined)
const prevQuestions = ref<
  {
    quizeQuestion: ReviewQuestionInstance
    badQuestionReason: string | undefined
  }[]
>([])

const scrollToBottom = () => {
  emit("need-scroll")
}

const contest = async () => {
  currentQuestionLegitMessage.value = ""
  const contestResult = await managedApi.restReviewQuestionController.contest(
    currentQuestion.value.id
  )

  if (!contestResult.rejected) {
    regenerating.value = true
    prevQuestions.value.push({
      quizeQuestion: currentQuestion.value,
      badQuestionReason: contestResult.reason,
    })
    currentQuestion.value =
      await managedApi.restReviewQuestionController.regenerate(
        currentQuestion.value.id
      )
  } else {
    currentQuestionLegitMessage.value = contestResult.reason
  }
  regenerating.value = false
  scrollToBottom()
}

const onAnswered = (answer: AnsweredQuestion) => {
  answeredQuestion.value = answer
  emit("answered", answeredQuestion.value)
}
</script>

<style lang="scss" scoped>
.review-question-instance {
  overflow-y: auto;
}
</style>

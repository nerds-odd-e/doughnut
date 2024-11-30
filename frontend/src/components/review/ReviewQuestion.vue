<template>
  <QuestionDisplay
    v-bind="{
      bareQuestion: reviewQuestionInstance.bareQuestion,
    }"
    @answer="submitAnswer($event)"
    :key="reviewQuestionInstance.id"
   />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { AnswerDTO, RecallPrompt } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import usePopups from "../commons/Popups/usePopups"
import QuestionDisplay from "../review/QuestionDisplay.vue"

const { managedApi } = useLoadingApi()
const { popups } = usePopups()

const props = defineProps({
  reviewQuestionInstance: {
    type: Object as PropType<RecallPrompt>,
    required: true,
  },
})

const emits = defineEmits(["answered"])

const submitAnswer = async (answerData: AnswerDTO) => {
  try {
    const answerResult =
      await managedApi.restReviewQuestionController.answerQuiz(
        props.reviewQuestionInstance.id,
        answerData
      )

    emits("answered", answerResult)
  } catch (_e) {
    await popups.alert(
      "This memory tracker doesn't exist any more or is being skipped now. Moving on to the next memory tracker..."
    )
  }
}
</script>

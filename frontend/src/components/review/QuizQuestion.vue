<template>
  <QuizQuestionDisplay
    v-bind="{
      quizQuestion,
      correctChoiceIndex,
      answerChoiceIndex,
      disabled,
      answeredCurrentQuestion,
    }"
    @answer="submitAnswer($event)"
    :key="quizQuestion.id"
   />
</template>

<script setup lang="ts">
import { PropType } from "vue"
import { AnswerDTO, QuizQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import usePopups from "../commons/Popups/usePopups"
import QuizQuestionDisplay from "../review/QuizQuestionDisplay.vue"

const { managedApi } = useLoadingApi()
const { popups } = usePopups()

const props = defineProps({
  quizQuestion: {
    type: Object as PropType<QuizQuestion>,
    required: true,
  },
  correctChoiceIndex: Number,
  answerChoiceIndex: Number,
  disabled: Boolean,
  answeredCurrentQuestion: Boolean,
})

const emits = defineEmits(["answered"])

const submitAnswer = async (answerData: AnswerDTO) => {
  try {
    const answerResult = await managedApi.restQuizQuestionController.answerQuiz(
      props.quizQuestion.id,
      answerData
    )

    emits("answered", answerResult)
  } catch (_e) {
    await popups.alert(
      "This review point doesn't exist any more or is being skipped now. Moving on to the next review point..."
    )
  }
}
</script>

<template>
  <div>
    <h3>Assessment For {{ topicConstructor }} </h3>
    <h5>If you achieve a score of more than {{ passCriteriaPercentage }}%, then you will receive the certificate</h5>
    <div>
      <div v-if="errors != ''">
        {{ errors }}
      </div>
      <QuizQuestionComp
        v-else-if="currentQuestion < quizQuestions.length"
        :quiz-question="quizQuestions[currentQuestion]!"
        @answered="questionAnswered"
      />
      <div v-else-if="assessmentCompleted">
        <p>Your score: {{ correctAnswers }} / {{ quizQuestions.length }}</p>
        <button :disabled="!assessmentPassed" class="btn btn-primary" @click="routeToCertificatePage()">
          Get Certificate
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { QuizQuestion, AnswerSubmission } from "@/generated/backend"
import { useRouter } from "vue-router"
import QuizQuestionComp from "@/components/review/QuizQuestion.vue"
import usePopups from "@/components/commons/Popups/usePopups.ts"

const { managedApi } = useLoadingApi()
const router = useRouter()
const props = defineProps({
  notebookId: { type: Number, required: true },
})
const { popups } = usePopups()
const topicConstructor = computed(() => {
  return router.currentRoute.value.query?.topic
})
const quizQuestions = ref<QuizQuestion[]>([])
const currentQuestion = ref(0)
const errors = ref("")
const correctAnswers = ref(0)
const questionsAnswerCollection = ref<AnswerSubmission[]>([])
const assessmentCompleted = computed(
  () =>
    currentQuestion.value >= quizQuestions.value.length &&
    quizQuestions.value.length > 0
)

const passCriteriaPercentage = 80

const assessmentPassed = computed(() => {
  const correctAnswersPercentage =
    (correctAnswers.value * 100) / quizQuestions.value.length
  return correctAnswersPercentage >= passCriteriaPercentage
})

const questionAnswered = async (answerResult) => {
  questionsAnswerCollection.value.push({
    questionId: quizQuestions.value[currentQuestion.value]!.id,
    answerId: answerResult.answerId,
    correctAnswers: answerResult.correct,
  })
  if (answerResult.correct) {
    correctAnswers.value += 1
  }
  currentQuestion.value += 1
  if (assessmentCompleted.value) {
    await managedApi.restAssessmentController.submitAssessmentResult(
      props.notebookId,
      questionsAnswerCollection.value
    )
  }
}

const generateAssessmentQuestions = () => {
  managedApi.restAssessmentController
    .generateAssessmentQuestions(props.notebookId)
    .then((response) => {
      quizQuestions.value = response
    })
    .catch((res) => {
      if (res.status === 403) {
        popups
          .alert(
            "You have reached the assessment limit for today. Please try again tomorrow."
          )
          .then(() => {
            router.back()
          })
      }
      errors.value = res.body.message
    })
}

const routeToCertificatePage = () => {
  router.push({
    name: "certificate",
    params: { notebookId: props.notebookId },
  })
}

onMounted(() => {
  generateAssessmentQuestions()
})
</script>

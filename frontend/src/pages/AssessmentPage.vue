<template>
  <h3>Assessment For {{ topicConstructor }}</h3>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { QuestionAnswerPair, QuizQuestion } from "@/generated/backend"
import { useRouter } from "vue-router"
import QuizQuestionComp from "@/components/review/QuizQuestion.vue"

const { managedApi } = useLoadingApi()
const router = useRouter()
const props = defineProps({
  notebookId: { type: Number, required: true },
})
const topicConstructor = computed(() => {
  return router.currentRoute.value.query?.topic
})
const quizQuestions = ref<QuizQuestion[]>([])
const currentQuestion = ref(0)
const questionsAnswerCollection = ref<QuestionAnswerPair[]>([])
const errors = ref("")
const correctAnswers = ref(0)
const assessmentCompleted = computed(
  () =>
    currentQuestion.value >= quizQuestions.value.length &&
    quizQuestions.value.length > 0
)

const questionAnswered = async (answerResult) => {
  questionsAnswerCollection.value.push({
    questionId: quizQuestions.value[currentQuestion.value]!.id,
    answerId: answerResult.answerId,
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
      errors.value = res.body.message
    })
}

onMounted(() => {
  generateAssessmentQuestions()
})
</script>

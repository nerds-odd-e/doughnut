<template>
  <div>
    <h3>Assessment For {{ topicConstructor }} </h3>
    <h5>Passing criteria: {{ passCriteriaPercentage }}%</h5>
    <div>
      <div v-if="errors != ''">
        {{ errors }}
      </div>
      <QuizQuestionComp v-else-if="currentQuestion < quizQuestions.length"
        :quiz-question="quizQuestions[currentQuestion]!" @answered="questionAnswered" :show-finetune-button="false" />
      <div v-else-if="assessmentResult">
        <p>Your score: {{ correctAnswers }} / {{ quizQuestions.length }}</p>
        <div class="alert alert-success" v-if="assessmentResult?.attempt?.isPass">
          You have passed the assessment.
        </div>
        <PopButton :disabled="!assessmentResult.isCertified" disabledTitle="This notebook does not award a certificate."
          btn-class="btn btn-light" title="View Certificate" v-if="assessmentResult.attempt?.isPass">
          <CertificatePopup :assessment-attempt="assessmentResult.attempt" :notebook-id="certificate?.notebook?.id">
          </CertificatePopup>
        </PopButton>
        <div class="alert alert-danger" v-else>
          You have not passed the assessment.
        </div>
      </div>
    </div>
  </div>
  <div :hidden="!answeredCurrentQuestion">
    <button class="btn btn-danger" @click="advance">Continue</button>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import {
  QuizQuestion,
  AnswerSubmission,
  AssessmentResult,
  Certificate,
} from "@/generated/backend"
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
const answeredCurrentQuestion = ref(false)
const errors = ref("")
const correctAnswers = ref(0)
const assessmentResult = ref<AssessmentResult | undefined>(undefined)
const questionsAnswerCollection = ref<AnswerSubmission[]>([])
const certificate = ref<Certificate>()

const passCriteriaPercentage = 80

const advance = () => {
  currentQuestion.value += 1
  answeredCurrentQuestion.value = false
  checkIfQuizComplete()
}
const questionAnswered = (answerResult) => {
  questionsAnswerCollection.value.push({
    questionId: quizQuestions.value[currentQuestion.value]!.id,
    answerId: answerResult.answerId,
    correctAnswers: answerResult.correct,
  })
  if (answerResult.correct) {
    correctAnswers.value += 1
    currentQuestion.value += 1
  } else {
    answeredCurrentQuestion.value = true
  }
  checkIfQuizComplete()
}

const checkIfQuizComplete = async () => {
  if (
    currentQuestion.value >= quizQuestions.value.length &&
    quizQuestions.value.length > 0
  ) {
    assessmentResult.value =
      await managedApi.restAssessmentController.submitAssessmentResult(
        props.notebookId,
        questionsAnswerCollection.value
      )
    if (
      assessmentResult.value.attempt?.isPass &&
      assessmentResult.value.isCertified
    ) {
      certificate.value =
        await managedApi.restCertificateController.claimCertificate(
          props.notebookId
        )
    }
  }
}

const generateAssessmentQuestions = async () => {
  try {
    quizQuestions.value =
      await managedApi.restAssessmentController.generateAssessmentQuestions(
        props.notebookId
      )
  } catch (err) {
    if (err instanceof Error) {
      errors.value = err.message
    } else {
      errors.value = String(err)
    }
  }
}

onMounted(() => {
  generateAssessmentQuestions()
})
</script>

<template>
  <div>
    <h5>Passing criteria: {{ passCriteriaPercentage }}%</h5>
    <div>
      <AssessmentQuestion
        v-if="currentQuestion < assessmentQuestionInstance.length"
        :answered-current-question="answeredCurrentQuestion"
        :assessment-question-instance="assessmentQuestionInstance[currentQuestion]!"
        @answered="questionAnswered"
      />
      <div v-else-if="assessmentResult">
        <p>Your score: {{ correctAnswers }} / {{ assessmentQuestionInstance.length }}</p>
        <div class="alert alert-success" v-if="assessmentResult?.attempt?.isPass">
          You have passed the assessment.
        </div>
        <PopButton
          :disabled="!assessmentResult.isCertified"
          disabledTitle="This notebook does not award a certificate."
          btn-class="btn btn-light"
          title="View Certificate"
          v-if="assessmentResult.attempt?.isPass"
        >
          <CertificatePopup
            :assessment-attempt="assessmentResult.attempt"
            :notebook-id="certificate?.notebook?.id"
          >
          </CertificatePopup>
        </PopButton>
        <div class="alert alert-danger" v-else>You have not passed the assessment.</div>
      </div>
    </div>
  </div>
  <div :hidden="!answeredCurrentQuestion">
    <button class="btn btn-danger" @click="advance">Continue</button>
    <PopButton title="Send feedback">
      <template #button_face>
        <button class="btn btn-secondary">Send feedback</button>
      </template>
      <template #default="{ closer }">
        <FeedbackForm
          @submitted="
            closer();
            handleFormSubmission();
          "
          :question="assessmentQuestionInstance[currentQuestion]?.reviewQuestionInstance"
        />
      </template>
    </PopButton>
    <div v-if="formSubmitted" class="alert alert-info">
      Feedback received successfully
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, PropType, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import {
  AnswerSubmission,
  AssessmentResult,
  Certificate,
  AssessmentAttempt,
} from "@/generated/backend"
import AssessmentQuestion from "./AssessmentQuestion.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  assessmentAttempt: {
    type: Object as PropType<AssessmentAttempt>,
    required: true,
  },
})

const currentQuestion = ref(0)
const answeredCurrentQuestion = ref(false)
const correctAnswers = ref(0)
const assessmentResult = ref<AssessmentResult | undefined>(undefined)
const questionsAnswerCollection = ref<AnswerSubmission[]>([])
const certificate = ref<Certificate>()
const formSubmitted = ref(false)
const assessmentQuestionInstance = computed(
  () => props.assessmentAttempt.assessmentQuestionInstances!
)

const passCriteriaPercentage = 80

const advance = () => {
  currentQuestion.value += 1
  answeredCurrentQuestion.value = false
  checkIfQuizComplete()
}
const questionAnswered = (answerResult) => {
  questionsAnswerCollection.value.push({
    questionId:
      assessmentQuestionInstance.value[currentQuestion.value]!
        .reviewQuestionInstance.id,
    answerId: answerResult.answerId,
    correctAnswers: answerResult.correct,
  })
  if (answerResult.correct) {
    correctAnswers.value += 1
    currentQuestion.value += 1
  } else {
    answeredCurrentQuestion.value = true
  }
  formSubmitted.value = false
  checkIfQuizComplete()
}

const checkIfQuizComplete = async () => {
  if (
    currentQuestion.value >= assessmentQuestionInstance.value.length &&
    assessmentQuestionInstance.value.length > 0
  ) {
    assessmentResult.value =
      await managedApi.restAssessmentController.submitAssessmentResult(
        props.assessmentAttempt.id,
        questionsAnswerCollection.value
      )
    if (
      assessmentResult.value.attempt?.isPass &&
      assessmentResult.value.isCertified
    ) {
      certificate.value =
        await managedApi.restCertificateController.claimCertificate(
          props.assessmentAttempt.notebookId
        )
    }
  }
}

const handleFormSubmission = () => {
  formSubmitted.value = true
}
</script>

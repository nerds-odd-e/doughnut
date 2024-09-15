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
        <div v-if="assessmentResult?.attempt?.isPass">
          <div class="alert alert-success">
            You have passed the assessment.
          </div>
          <AssessmentClaimCertificate
            v-if="assessmentResult.certified"
            :notebook-id="assessmentResult.notebookId!"
          />
          <i v-else> (This is not a certifiable assessment.)</i>
        </div>
        <div class="alert alert-danger" v-else="">You have not passed the assessment.</div>
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
import { AssessmentResult, AssessmentAttempt } from "@/generated/backend"
import AssessmentQuestion from "./AssessmentQuestion.vue"
import AssessmentClaimCertificate from "./AssessmentClaimCertificate.vue"

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
  // questionsAnswerCollection.value.push({
  //   questionId:
  //     assessmentQuestionInstance.value[currentQuestion.value]!
  //       .reviewQuestionInstance.id,
  //   answerId: answerResult.answerId,
  //   correctAnswers: answerResult.correct,
  // })
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
        props.assessmentAttempt.id
      )
  }
}

const handleFormSubmission = () => {
  formSubmitted.value = true
}
</script>

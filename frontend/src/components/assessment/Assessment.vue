<template>
  <div>
    <h5>Passing criteria: {{ passCriteriaPercentage }}%</h5>
    <div>
      <AssessmentQuestion
        v-if="currentQuestion < assessmentQuestionInstance.length"
        :assessment-question-instance="assessmentQuestionInstance[currentQuestion]!"
        @advance="advance"
        :key="currentQuestion"
      />
      <div v-else-if="assessmentResult">
        <p>Your score: {{ assessmentResult.correctCount }} / {{ assessmentResult.totalCount }}</p>
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
const assessmentResult = ref<AssessmentResult | undefined>(undefined)
const assessmentQuestionInstance = computed(
  () => props.assessmentAttempt.assessmentQuestionInstances!
)

const passCriteriaPercentage = 80

const advance = () => {
  currentQuestion.value += 1
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
</script>

<template>
  <div>
    <h5>Passing criteria: {{ passCriteriaPercentage }}%</h5>
    <div>
      <AssessmentQuestion
        v-if="currentQuestion < assessmentQuestionInstance.length"
        :assessment-question-instance="currentQuestionInstance"
        @advance="advance"
        :key="currentQuestion"
      />
      <div v-else-if="localAssessmentAttempt?.submittedAt">
        <p>Your score: {{ localAssessmentAttempt.answersCorrect }} / {{ localAssessmentAttempt.totalQuestionCount }}</p>
        <div v-if="localAssessmentAttempt.isPass">
          <div class="daisy-alert daisy-alert-success">
            You have passed the assessment.
          </div>
          <AssessmentClaimCertificate
            v-if="localAssessmentAttempt.certifiable"
            :notebook-id="localAssessmentAttempt.notebookId!"
          />
          <i v-else> (This is not a certifiable assessment.)</i>
        </div>
        <div class="daisy-alert daisy-alert-danger" v-else="">You have not passed the assessment.</div>
      </div>
    </div>
  </div>

</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, ref } from "vue"
import { AssessmentController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { AssessmentAttempt } from "@generated/backend"
import AssessmentQuestion from "./AssessmentQuestion.vue"
import AssessmentClaimCertificate from "./AssessmentClaimCertificate.vue"
const props = defineProps({
  assessmentAttempt: {
    type: Object as PropType<AssessmentAttempt>,
    required: true,
  },
})

const currentQuestion = ref(0)
const localAssessmentAttempt = ref<AssessmentAttempt | undefined>(
  props.assessmentAttempt
)
const assessmentQuestionInstance = computed(
  () => props.assessmentAttempt.assessmentQuestionInstances!
)

const currentQuestionInstance = computed(() => {
  return assessmentQuestionInstance.value[currentQuestion.value]!
})

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
    const { data: submittedAttempt, error } = await apiCallWithLoading(() =>
      AssessmentController.submitAssessmentResult({
        path: { assessmentAttempt: props.assessmentAttempt.id },
      })
    )
    if (!error && submittedAttempt) {
      localAssessmentAttempt.value = submittedAttempt
    }
  }
}
</script>

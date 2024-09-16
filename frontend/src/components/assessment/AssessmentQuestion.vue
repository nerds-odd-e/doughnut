<template>
  <QuestionDisplay
    v-bind="{
      bareQuestion: localAssessmentQuestionInstance.bareQuestion,
      answeredCurrentQuestion: localAssessmentQuestionInstance.answered,
    }"
    @answer="submitAnswer($event)"
    :key="localAssessmentQuestionInstance.id"
   />
  <div :hidden="!localAssessmentQuestionInstance.answered">
    <button class="btn btn-danger" @click="$emit('advance')">Continue</button>
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
          :question="localAssessmentQuestionInstance"
        />
      </template>
    </PopButton>
    <div v-if="formSubmitted" class="alert alert-info">
      Feedback received successfully
    </div>
  </div>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue"
import { AnswerDTO, AssessmentQuestionInstance } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import usePopups from "../commons/Popups/usePopups"
import QuestionDisplay from "../review/QuestionDisplay.vue"

const { managedApi } = useLoadingApi()
const { popups } = usePopups()

const props = defineProps({
  assessmentQuestionInstance: {
    type: Object as PropType<AssessmentQuestionInstance>,
    required: true,
  },
})

const localAssessmentQuestionInstance = ref(props.assessmentQuestionInstance)
const formSubmitted = ref(false)

const emits = defineEmits(["advance"])

const submitAnswer = async (answerData: AnswerDTO) => {
  try {
    localAssessmentQuestionInstance.value =
      await managedApi.restAssessmentController.answerQuestion(
        props.assessmentQuestionInstance.id,
        answerData
      )

    if (localAssessmentQuestionInstance.value.answeredCorrectly) {
      emits("advance")
    }
  } catch (_e) {
    await popups.alert(
      "This review point doesn't exist any more or is being skipped now. Moving on to the next review point..."
    )
  }
}

const handleFormSubmission = () => {
  formSubmitted.value = true
}
</script>

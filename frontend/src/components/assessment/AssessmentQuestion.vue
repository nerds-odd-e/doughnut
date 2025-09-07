<template>
  <QuestionDisplay
    v-bind="{
      multipleChoicesQuestion: localAssessmentQuestionInstance.multipleChoicesQuestion,
      answer: localAssessmentQuestionInstance.answer,
    }"
    @answer="submitAnswer($event)"
    :key="localAssessmentQuestionInstance.id"
   />
  <div v-if="localAssessmentQuestionInstance.answer && !localAssessmentQuestionInstance.answer.correct">
    <div class="daisy-alert daisy-alert-danger">
      <strong>The answer is incorrect.</strong>
    </div>
    <button class="daisy-btn daisy-btn-danger" @click="$emit('advance')">Continue</button>
    <PopButton title="Send feedback">
      <template #button_face>
        <button class="daisy-btn daisy-btn-secondary">Send feedback</button>
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
    <div v-if="formSubmitted" class="daisy-alert daisy-alert-info">
      Feedback received successfully
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { AnswerDTO, AssessmentQuestionInstance } from "@generated/backend"
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

    if (localAssessmentQuestionInstance.value.answer?.correct) {
      emits("advance")
    }
  } catch (_e) {
    await popups.alert(
      "This memory tracker doesn't exist any more or is being skipped now. Moving on to the next memory tracker..."
    )
  }
}

const handleFormSubmission = () => {
  formSubmitted.value = true
}
</script>

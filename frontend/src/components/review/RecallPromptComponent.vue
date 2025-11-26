<template>
  <div class="daisy-relative">
    <QuestionDisplay
      v-bind="{
        multipleChoicesQuestion: recallPrompt.multipleChoicesQuestion,
      }"
      @answer="submitQuizAnswer($event)"
      :key="recallPrompt.id"
      :question-id="recallPrompt.id"
      :disabled="isLoading || isAnswered"
    />

    <div v-if="isAnswered" data-test="answered-overlay" class="daisy-absolute daisy-inset-0 daisy-bg-base-100/80 daisy-z-50">
      <div v-if="isLoading" class="daisy-flex daisy-justify-center daisy-items-center daisy-h-full">
        <span class="daisy-loading daisy-loading-spinner daisy-loading-lg"></span>
      </div>
    </div>

    <div v-if="error" class="daisy-alert daisy-alert-error daisy-mt-4">
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { PropType } from "vue"
import type { AnswerDto, RecallPrompt } from "@generated/backend"
import { answerQuiz } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import QuestionDisplay from "./QuestionDisplay.vue"

const { popups } = usePopups()

const isLoading = ref(false)
const isAnswered = ref(false)
const error = ref("")

const props = defineProps({
  recallPrompt: {
    type: Object as PropType<RecallPrompt>,
    required: true,
  },
})

const emits = defineEmits(["answered"])

const handleError = async () => {
  error.value = "Failed to submit answer. Please try again."
  await popups.alert(
    "This memory tracker doesn't exist any more or is being skipped now. Moving on to the next memory tracker..."
  )
}

const submitQuizAnswer = async (answerData: AnswerDto) => {
  if (answerData.choiceIndex === undefined) return

  isAnswered.value = true
  isLoading.value = true
  error.value = ""

  const { data: answerResult, error: apiError } = await apiCallWithLoading(
    (client) =>
      answerQuiz({
        path: { recallPrompt: props.recallPrompt.id },
        body: answerData,
        client,
      })
  )
  if (!apiError) {
    emits("answered", answerResult!)
  } else {
    await handleError()
    isAnswered.value = false
  }
  isLoading.value = false
}
</script>

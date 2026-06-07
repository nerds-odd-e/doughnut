<template>
  <div class="relative">
    <QuestionDisplay
      v-if="recallPrompt.multipleChoicesQuestion"
      v-bind="{
        multipleChoicesQuestion: recallPrompt.multipleChoicesQuestion,
      }"
      @answer="submitQuizAnswer($event)"
      :key="recallPrompt.id"
      :disabled="isLoading || isAnswered"
    />

    <div v-if="isAnswered" data-test="answered-overlay" class="absolute inset-0 bg-base-100/80 z-50">
      <div v-if="isLoading" class="flex justify-center items-center h-full">
        <span class="daisy-loading daisy-loading-spinner daisy-loading-lg"></span>
      </div>
    </div>

    <div v-if="error" class="daisy-alert daisy-alert-error mt-4">
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { PropType } from "vue"
import type {
  AnsweredQuestion,
  AnswerDto,
  RecallQuestion,
} from "@generated/doughnut-backend-api"
import { RecallPromptController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import QuestionDisplay from "./QuestionDisplay.vue"
import { primeSoftKeyboard } from "@/utils/focusTarget"

const { popups } = usePopups()

const isLoading = ref(false)
const isAnswered = ref(false)
const error = ref("")

const props = defineProps({
  recallPrompt: {
    type: Object as PropType<RecallQuestion>,
    required: true,
  },
  nextIsSpelling: {
    type: Boolean,
    default: false,
  },
})

const emits = defineEmits<{
  (e: "answered", result: AnsweredQuestion): void
}>()

const handleError = async () => {
  error.value = "Failed to submit answer. Please try again."
  await popups.alert(
    "This memory tracker doesn't exist any more or is being skipped now. Moving on to the next memory tracker..."
  )
}

const submitQuizAnswer = async (answerData: AnswerDto) => {
  if (answerData.choiceIndex === undefined) return
  if (props.nextIsSpelling) primeSoftKeyboard()

  isAnswered.value = true
  isLoading.value = true
  error.value = ""

  const { data: answerResult, error: apiError } = await apiCallWithLoading(() =>
    RecallPromptController.answerQuiz({
      path: { recallPrompt: props.recallPrompt.id },
      body: answerData,
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

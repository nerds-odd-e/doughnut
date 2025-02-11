<template>
  <div class="daisy-relative">
    <SpellingQuestionDisplay
      v-if="!recallPrompt.bareQuestion.multipleChoicesQuestion.choices || recallPrompt.bareQuestion.multipleChoicesQuestion.choices.length === 0"
      v-bind="{
        bareQuestion: recallPrompt.bareQuestion,
      }"
      @answer="submitSpellingAnswer($event)"
      :key="`spelling-${recallPrompt.id}`"
    />
    <QuestionDisplay
      v-else
      v-bind="{
        bareQuestion: recallPrompt.bareQuestion,
      }"
      @answer="submitQuizAnswer($event)"
      :key="`choice-${recallPrompt.id}`"
      :disabled="isLoading"
    />

    <div v-if="isLoading" class="daisy-absolute daisy-inset-0 daisy-bg-base-100/80 daisy-flex daisy-justify-center daisy-items-center daisy-z-50">
      <span class="daisy-loading daisy-loading-spinner daisy-loading-lg"></span>
    </div>

    <div v-if="error" class="daisy-alert daisy-alert-error daisy-mt-4">
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { PropType } from "vue"
import type {
  AnswerDTO,
  RecallPrompt,
  AnswerSpellingDTO,
} from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import usePopups from "../commons/Popups/usePopups"
import QuestionDisplay from "./QuestionDisplay.vue"
import SpellingQuestionDisplay from "./SpellingQuestionDisplay.vue"

const { managedApi } = useLoadingApi()
const { popups } = usePopups()

const isLoading = ref(false)
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

const submitSpellingAnswer = async (answerData: AnswerSpellingDTO) => {
  if (answerData.spellingAnswer === undefined) return

  isLoading.value = true
  error.value = ""

  try {
    const answerResult =
      await managedApi.restRecallPromptController.answerSpelling(
        props.recallPrompt.id,
        { spellingAnswer: answerData.spellingAnswer }
      )
    emits("answered", answerResult)
  } catch (e) {
    await handleError()
  } finally {
    isLoading.value = false
  }
}

const submitQuizAnswer = async (answerData: AnswerDTO) => {
  if (answerData.choiceIndex === undefined) return

  isLoading.value = true
  error.value = ""

  try {
    const answerResult = await managedApi.restRecallPromptController.answerQuiz(
      props.recallPrompt.id,
      answerData
    )
    emits("answered", answerResult)
  } catch (e) {
    await handleError()
  } finally {
    isLoading.value = false
  }
}
</script>

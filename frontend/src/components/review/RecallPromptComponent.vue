<template>
  <div class="position-relative">
    <QuestionDisplay
      v-bind="{
        bareQuestion: recallPrompt.bareQuestion,
      }"
      @answer="submitAnswer($event)"
      :key="recallPrompt.id"
      :disabled="isLoading"
    />

    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-spinner">
        <i class="fas fa-spinner fa-spin fa-2x"></i>
      </div>
    </div>

    <div v-if="error" class="error-message alert alert-danger">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { PropType } from "vue"
import type { AnswerDTO, RecallPrompt } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import usePopups from "../commons/Popups/usePopups"
import QuestionDisplay from "../review/QuestionDisplay.vue"

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

const submitAnswer = async (answerData: AnswerDTO) => {
  isLoading.value = true
  error.value = ""

  try {
    const answerResult = await managedApi.restRecallPromptController.answerQuiz(
      props.recallPrompt.id,
      answerData
    )

    emits("answered", answerResult)
  } catch (e) {
    error.value = "Failed to submit answer. Please try again."
    await popups.alert(
      "This memory tracker doesn't exist any more or is being skipped now. Moving on to the next memory tracker..."
    )
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.error-message {
  margin-top: 1rem;
}
</style>

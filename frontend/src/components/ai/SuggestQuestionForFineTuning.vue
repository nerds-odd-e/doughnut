<template>
  <h2>Suggest This Question For AI Fine Tuning</h2>
  <p>
    <i
      >Sending this question for fine tuning the question generation model will
      make this note and question visible to admin. Are you sure?</i
    >
  </p>
  <div>
    <label> This question is: </label>
    <button
      class="positive-feedback-btn feedback-btn"
      :class="{ selected: params.isPositiveFeedback }"
      @click="params.isPositiveFeedback = true"
    >
      👍 Good
    </button>
    <button
      class="negative-feedback-btn feedback-btn"
      :class="{ selected: !params.isPositiveFeedback }"
      @click="params.isPositiveFeedback = false"
    >
      👎 Bad
    </button>
  </div>
  <TextInput
    id="feedback-comment"
    field="comment"
    v-model="params.comment"
    placeholder="Add a comment about the question"
  />
  <div class="feedback-actions-container">
    <button
      class="suggest-fine-tuning-ok-btn btn btn-success"
      @click="suggestQuestionForFineTuning"
    >
      OK
    </button>
  </div>
</template>
<script setup lang="ts">
import {
  QuestionSuggestionCreationParams,
  QuizQuestion,
} from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { ref } from "vue"

const params = ref<QuestionSuggestionCreationParams>({
  isPositiveFeedback: false,
  comment: "",
})
const { managedApi } = useLoadingApi()

const props = defineProps<{
  quizQuestion: QuizQuestion
}>()

const emit = defineEmits(["closeDialog"])

const { quizQuestion } = props

async function suggestQuestionForFineTuning() {
  await managedApi.restQuizQuestionController.suggestQuestionForFineTuning(
    quizQuestion.id,
    params.value
  )
  emit("closeDialog")
}
</script>
<style scoped>
.feedback-btn {
  background-color: #007bff;
  color: white;
  padding: 5px;
  margin: 5px;
  border-radius: 5px;
}
.positive-feedback-btn.feedback-btn.selected {
  background-color: green;
}
.negative-feedback-btn.feedback-btn.selected {
  background-color: red;
}
.feedback-actions-container {
  display: flex;
  align-items: center;
}
</style>

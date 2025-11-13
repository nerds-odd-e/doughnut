<template>
  <div class="daisy-modal-box">
    <h3 class="daisy-font-bold daisy-text-lg daisy-mb-4">Edit Question</h3>
    
    <form @submit.prevent="saveQuestion">
      <!-- Question Stem -->
      <div class="daisy-form-control daisy-mb-4">
        <label class="daisy-label">
          <span class="daisy-label-text">Question Text</span>
        </label>
        <textarea
          v-model="editedQuestion.multipleChoicesQuestion.stem"
          class="daisy-textarea daisy-textarea-bordered daisy-w-full"
          placeholder="Enter question text"
          rows="3"
          required
        ></textarea>
      </div>

      <!-- Answer Choices -->
      <div class="daisy-form-control daisy-mb-4">
        <label class="daisy-label">
          <span class="daisy-label-text">Answer Choices</span>
        </label>
        <div class="daisy-space-y-2">
          <div
            v-for="(choice, index) in editedQuestion.multipleChoicesQuestion.choices"
            :key="index"
            class="daisy-flex daisy-items-center daisy-gap-2"
          >
            <input
              type="radio"
              :name="'correct-answer'"
              :value="index"
              v-model="editedQuestion.correctAnswerIndex"
              class="daisy-radio daisy-radio-primary"
            />
            <input
              v-model="editedQuestion.multipleChoicesQuestion.choices[index]"
              type="text"
              class="daisy-input daisy-input-bordered daisy-flex-1"
              :placeholder="`Choice ${String.fromCharCode(65 + index)}`"
              required
            />
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="daisy-modal-action">
        <button
          type="button"
          class="daisy-btn"
          @click="$emit('close')"
        >
          Cancel
        </button>
        <button
          type="submit"
          class="daisy-btn daisy-btn-primary"
          :disabled="!isFormValid"
        >
          Save Changes
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { PropType } from 'vue'
import type { PredefinedQuestion } from '@generated/backend'
import useLoadingApi from '@/managedApi/useLoadingApi'

const { managedApi } = useLoadingApi()

const props = defineProps({
  question: {
    type: Object as PropType<PredefinedQuestion>,
    required: true,
  },
})

const emit = defineEmits<{
  close: []
  questionUpdated: [question: PredefinedQuestion]
}>()

// Create a deep copy of the question for editing
const editedQuestion = ref<PredefinedQuestion>({
  ...props.question,
  multipleChoicesQuestion: {
    ...props.question.multipleChoicesQuestion,
    choices: [...(props.question.multipleChoicesQuestion.choices || [])],
  },
})

// Watch for prop changes to update the edited question
watch(
  () => props.question,
  (newQuestion) => {
    editedQuestion.value = {
      ...newQuestion,
      multipleChoicesQuestion: {
        ...newQuestion.multipleChoicesQuestion,
        choices: [...(newQuestion.multipleChoicesQuestion.choices || [])],
      },
    }
  },
  { deep: true }
)

// Form validation
const isFormValid = computed(() => {
  const stem = editedQuestion.value.multipleChoicesQuestion.stem?.trim()
  const choices = editedQuestion.value.multipleChoicesQuestion.choices || []
  const hasValidChoices = choices.every(choice => choice?.trim())
  const hasCorrectAnswer = editedQuestion.value.correctAnswerIndex !== undefined && 
                          editedQuestion.value.correctAnswerIndex >= 0 && 
                          editedQuestion.value.correctAnswerIndex < choices.length
  
  return stem && hasValidChoices && hasCorrectAnswer
})

// Save the edited question
const saveQuestion = async () => {
  try {
    const updatedQuestion = await managedApi.restPredefinedQuestionController.updateQuestion(
      editedQuestion.value.id!,
      editedQuestion.value
    )
    emit('questionUpdated', updatedQuestion)
    emit('close')
  } catch (error) {
    console.error('Error updating question:', error)
    // Handle error (could show a toast notification)
  }
}
</script>

<style scoped>
.daisy-space-y-2 > * + * {
  margin-top: 0.5rem;
}
</style>
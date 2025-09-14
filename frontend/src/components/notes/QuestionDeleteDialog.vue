<template>
  <div class="daisy-modal-box">
    <h3 class="daisy-font-bold daisy-text-lg daisy-mb-4">Delete Question</h3>
    
    <div class="daisy-mb-6">
      <p class="daisy-text-base daisy-mb-4">
        Are you sure you want to delete this question? This action cannot be undone.
      </p>
      
      <!-- Show question preview -->
      <div class="daisy-bg-base-200 daisy-p-4 daisy-rounded-lg">
        <h4 class="daisy-font-semibold daisy-mb-2">Question:</h4>
        <p class="daisy-text-sm daisy-mb-3">{{ question.multipleChoicesQuestion.stem }}</p>
        
        <h4 class="daisy-font-semibold daisy-mb-2">Choices:</h4>
        <ul class="daisy-text-sm">
          <li
            v-for="(choice, index) in question.multipleChoicesQuestion.choices"
            :key="index"
            :class="{
              'daisy-font-bold daisy-text-success': index === question.correctAnswerIndex
            }"
          >
            {{ String.fromCharCode(65 + index) }}. {{ choice }}
            <span v-if="index === question.correctAnswerIndex" class="daisy-text-success">
              (Correct Answer)
            </span>
          </li>
        </ul>
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
        type="button"
        class="daisy-btn daisy-btn-error"
        @click="deleteQuestion"
        :disabled="isDeleting"
      >
        <span v-if="isDeleting" class="daisy-loading daisy-loading-spinner daisy-loading-sm"></span>
        {{ isDeleting ? 'Deleting...' : 'Delete Question' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
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
  questionDeleted: [questionId: number]
}>()

const isDeleting = ref(false)

// Delete the question
const deleteQuestion = async () => {
  if (!props.question.id) return
  
  isDeleting.value = true
  try {
    await managedApi.restPredefinedQuestionController.deleteQuestion(props.question.id)
    emit('questionDeleted', props.question.id)
    emit('close')
  } catch (error) {
    console.error('Error deleting question:', error)
    // Handle error (could show a toast notification)
  } finally {
    isDeleting.value = false
  }
}
</script>
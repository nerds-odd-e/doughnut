<template>
  <div class="question-management">
    <div class="mb-3">
      <label for="questionText" class="form-label">質問文</label>
      <textarea
        id="questionText"
        v-model="editedQuestion.multipleChoicesQuestion.stem"
        class="form-control"
        rows="3"
      ></textarea>
    </div>
    <div class="mb-3">
      <label class="form-label">選択肢</label>
      <div v-for="(choice, index) in editedQuestion.multipleChoicesQuestion.choices" :key="index" class="input-group mb-2">
        <span class="input-group-text">
          <input
            type="radio"
            :checked="editedQuestion.correctAnswerIndex === index"
            @change="editedQuestion.correctAnswerIndex = index"
            :name="'correct-answer'"
            :id="'choice-' + index"
          >
        </span>
        <input
          type="text"
          v-model="editedQuestion.multipleChoicesQuestion.choices[index]"
          class="form-control"
          :placeholder="'選択肢 ' + (index + 1)"
        >
      </div>
    </div>
    <div class="d-flex justify-content-between">
      <button class="btn btn-primary" @click="saveQuestion">保存</button>
      <div>
        <PopButton btn-class="btn-secondary me-2">
          <template #button_face>
            AIモデルの改善に使用
          </template>
          <template #default="{ closer }">
            <SuggestQuestionForFineTuning
              :predefined-question="predefinedQuestion"
              @close-dialog="closer()"
            />
          </template>
        </PopButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { PredefinedQuestion } from "@/generated/backend"
import SuggestQuestionForFineTuning from "../ai/SuggestQuestionForFineTuning.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import PopButton from "../commons/Popups/PopButton.vue"

const props = defineProps({
  predefinedQuestion: {
    type: Object as PropType<PredefinedQuestion>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: 'question-updated', question: PredefinedQuestion): void
}>()

const { managedApi } = useLoadingApi()
const editedQuestion = ref<PredefinedQuestion>({
  ...props.predefinedQuestion,
  multipleChoicesQuestion: {
    ...props.predefinedQuestion.multipleChoicesQuestion,
    choices: [...props.predefinedQuestion.multipleChoicesQuestion.choices]
  }
})

const saveQuestion = async () => {
  try {
    const updatedQuestion = await managedApi.restPredefinedQuestionController.updateQuestion(
      props.predefinedQuestion.id,
      editedQuestion.value
    )
    emit('question-updated', updatedQuestion)
  } catch (error) {
    console.error('Failed to update question:', error)
  }
}
</script>

<style scoped>
.question-management {
  padding: 1rem;
}
</style>

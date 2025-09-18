<template>
  <div>
    <PopButton btn-class="daisy-btn daisy-btn-primary" title="Add Question">
      <template #default="{ closer }">
        <NoteAddQuestion
          v-bind="{ note }"
          @close-dialog="
            closer();
            questionAdded($event);
          "
        />
      </template>
    </PopButton>
    <table class="question-table daisy-mt-2" v-if="questions.length">
      <thead>
        <tr>
          <th>Approved</th>
          <th>Question Text</th>
          <th>A</th>
          <th>B</th>
          <th>C</th>
          <th>D</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(question, outerIndex) in questions"
          :key="question.multipleChoicesQuestion.stem"
        >
          <td>
            <input
              :id="'checkbox-' + outerIndex"
              type="checkbox"
              v-model="question.approved"
              @change="toggleApproval(question.id)"
            />
          </td>
          <td>
            <span @click="openedQuestion=question">
              {{ question.multipleChoicesQuestion.stem }}
            </span>
          </td>
          <template
            v-if="question.multipleChoicesQuestion.choices"
          >
            <td
              v-for="(choice, index) in question
                .multipleChoicesQuestion.choices"
              :class="{
                'correct-choice': index === question.correctAnswerIndex,
              }"
              :key="index"
            >
              {{ choice }}
            </td>
          </template>
          <td>
            <div class="daisy-btn-group">
              <button
                class="daisy-btn daisy-btn-sm daisy-btn-outline"
                @click="openEditModal(question)"
                title="Edit Question"
              >
                Edit
              </button>
              <button
                class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-btn-error"
                @click="openDeleteModal(question)"
                title="Delete Question"
              >
                Delete
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="daisy-mt-2 daisy-w-full daisy-text-center">
      <b>No questions</b>
    </div>
  </div>
  <Modal
    v-if="openedQuestion !== undefined"
    @close_request="openedQuestion = undefined"
  >
    <template #body>
      <QuestionManagement
        :predefinedQuestion="openedQuestion"
      />
    </template>
  </Modal>

  <!-- Edit Question Modal -->
  <Modal
    v-if="editingQuestion !== undefined"
    @close_request="editingQuestion = undefined"
  >
    <template #body>
      <QuestionEditModal
        :question="editingQuestion"
        @close="editingQuestion = undefined"
        @question-updated="onQuestionUpdated"
      />
    </template>
  </Modal>

  <!-- Delete Question Modal -->
  <Modal
    v-if="deletingQuestion !== undefined"
    @close_request="deletingQuestion = undefined"
  >
    <template #body>
      <QuestionDeleteDialog
        :question="deletingQuestion"
        @close="deletingQuestion = undefined"
        @question-deleted="onQuestionDeleted"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import type { Note, PredefinedQuestion } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteAddQuestion from "./NoteAddQuestion.vue"
import QuestionManagement from "./QuestionManagement.vue"
import QuestionEditModal from "./QuestionEditModal.vue"
import QuestionDeleteDialog from "./QuestionDeleteDialog.vue"
import PopButton from "../commons/Popups/PopButton.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})
const questions = ref<PredefinedQuestion[]>([])
const openedQuestion = ref<PredefinedQuestion | undefined>()
const editingQuestion = ref<PredefinedQuestion | undefined>()
const deletingQuestion = ref<PredefinedQuestion | undefined>()

const fetchQuestions = async () => {
  questions.value =
    await managedApi.restPredefinedQuestionController.getAllQuestionByNote(
      props.note.id
    )
}
const questionAdded = (newQuestion: PredefinedQuestion) => {
  if (newQuestion == null) {
    return
  }
  questions.value.push(newQuestion)
}
const toggleApproval = async (questionId?: number) => {
  if (questionId) {
    await managedApi.restPredefinedQuestionController.toggleApproval(questionId)
  }
}

const openEditModal = (question: PredefinedQuestion) => {
  editingQuestion.value = question
}

const openDeleteModal = (question: PredefinedQuestion) => {
  deletingQuestion.value = question
}

const onQuestionUpdated = (updatedQuestion: PredefinedQuestion) => {
  const index = questions.value.findIndex(q => q.id === updatedQuestion.id)
  if (index !== -1) {
    questions.value[index] = updatedQuestion
  }
}

const onQuestionDeleted = (questionId: number) => {
  questions.value = questions.value.filter(q => q.id !== questionId)
}

onMounted(() => {
  fetchQuestions()
})
</script>

<style scoped>
.question-table {
  border-collapse: collapse;
  width: 100%;
}

.question-table th,
.question-table td {
  border: 1px solid #dddddd;
  text-align: left;
  padding: 8px;
}

.question-table th {
  background-color: #f2f2f2;
}

.correct-choice {
  background-color: #4caf50;
}
</style>

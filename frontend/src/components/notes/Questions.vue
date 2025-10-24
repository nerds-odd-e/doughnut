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
            <button
              class="daisy-btn daisy-btn-sm daisy-btn-primary daisy-mr-2"
              @click="editQuestion(question)"
              title="Edit question"
            >
              Edit
            </button>
            <button
              class="daisy-btn daisy-btn-sm daisy-btn-error"
              @click="deleteQuestion(question.id)"
              title="Delete question"
            >
              Delete
            </button>
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
  <Modal
    v-if="editingQuestion !== undefined"
    @close_request="editingQuestion = undefined"
  >
    <template #body>
      <div class="daisy-p-4">
        <h3 class="daisy-text-lg daisy-font-bold daisy-mb-4">Edit Question</h3>
        <form @submit.prevent="saveEditedQuestion">
          <div class="daisy-form-control daisy-mb-4">
            <label class="daisy-label" for="edit-question-stem">
              <span class="daisy-label-text">Question Text</span>
            </label>
            <textarea
              id="edit-question-stem"
              v-model="editForm.stem"
              class="daisy-textarea daisy-textarea-bordered daisy-w-full"
              rows="3"
              required
            ></textarea>
          </div>
          <div class="daisy-form-control daisy-mb-4">
            <label class="daisy-label" for="edit-choice-a">
              <span class="daisy-label-text">Choice A</span>
            </label>
            <input
              id="edit-choice-a"
              v-model="editForm.choices[0]"
              type="text"
              class="daisy-input daisy-input-bordered daisy-w-full"
              required
            />
          </div>
          <div class="daisy-form-control daisy-mb-4">
            <label class="daisy-label" for="edit-choice-b">
              <span class="daisy-label-text">Choice B</span>
            </label>
            <input
              id="edit-choice-b"
              v-model="editForm.choices[1]"
              type="text"
              class="daisy-input daisy-input-bordered daisy-w-full"
              required
            />
          </div>
          <div class="daisy-form-control daisy-mb-4">
            <label class="daisy-label" for="edit-choice-c">
              <span class="daisy-label-text">Choice C</span>
            </label>
            <input
              id="edit-choice-c"
              v-model="editForm.choices[2]"
              type="text"
              class="daisy-input daisy-input-bordered daisy-w-full"
              required
            />
          </div>
          <div class="daisy-form-control daisy-mb-4">
            <label class="daisy-label" for="edit-choice-d">
              <span class="daisy-label-text">Choice D</span>
            </label>
            <input
              id="edit-choice-d"
              v-model="editForm.choices[3]"
              type="text"
              class="daisy-input daisy-input-bordered daisy-w-full"
              required
            />
          </div>
          <div class="daisy-form-control daisy-mb-4">
            <label class="daisy-label" for="edit-correct-answer">
              <span class="daisy-label-text">Correct Answer</span>
            </label>
            <select
              id="edit-correct-answer"
              v-model="editForm.correctAnswerIndex"
              class="daisy-select daisy-select-bordered daisy-w-full"
              required
            >
              <option :value="0">A</option>
              <option :value="1">B</option>
              <option :value="2">C</option>
              <option :value="3">D</option>
            </select>
          </div>
          <div class="daisy-flex daisy-gap-2 daisy-justify-end">
            <button
              type="button"
              class="daisy-btn"
              @click="editingQuestion = undefined"
            >
              Cancel
            </button>
            <button
              type="submit"
              class="daisy-btn daisy-btn-primary"
            >
              Save
            </button>
          </div>
        </form>
      </div>
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
import PopButton from "../commons/Popups/PopButton.vue"
import Modal from "../commons/Modal.vue"

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
const editForm = ref({
  stem: "",
  choices: ["", "", "", ""],
  correctAnswerIndex: 0,
})

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
const deleteQuestion = async (questionId?: number) => {
  if (questionId) {
    await managedApi.restPredefinedQuestionController.deleteQuestion(questionId)
    questions.value = questions.value.filter((q) => q.id !== questionId)
  }
}
const editQuestion = (question: PredefinedQuestion) => {
  editingQuestion.value = question
  editForm.value = {
    stem: question.multipleChoicesQuestion.stem || "",
    choices: [
      ...(question.multipleChoicesQuestion.choices || ["", "", "", ""]),
    ],
    correctAnswerIndex: question.correctAnswerIndex || 0,
  }
}
const saveEditedQuestion = async () => {
  if (editingQuestion.value?.id) {
    const updatedQuestion: PredefinedQuestion = {
      ...editingQuestion.value,
      multipleChoicesQuestion: {
        stem: editForm.value.stem,
        choices: editForm.value.choices,
      },
      correctAnswerIndex: editForm.value.correctAnswerIndex,
    }
    const result =
      await managedApi.restPredefinedQuestionController.updateQuestion(
        editingQuestion.value.id,
        updatedQuestion
      )
    // Update the question in the list
    const index = questions.value.findIndex(
      (q) => q.id === editingQuestion.value?.id
    )
    if (index !== -1) {
      questions.value[index] = result
    }
    editingQuestion.value = undefined
  }
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

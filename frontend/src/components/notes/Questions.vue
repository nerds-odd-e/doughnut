<template>
  <div>
    <PopButton btn-class="btn btn-primary" title="Add Question">
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
    <table class="question-table mt-2" v-if="questions.length">
      <thead>
        <tr>
          <th>Approved</th>
          <th>Question Text</th>
          <th>A</th>
          <th>B</th>
          <th>C</th>
          <th>D</th>
          <th>Action</th>
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
            <template v-for="(_, index) in Array(4)" :key="index">
              <td v-if="index < question.multipleChoicesQuestion.choices.length"
                :class="{
                  'correct-choice': index === question.correctAnswerIndex,
                }">
                {{ question.multipleChoicesQuestion.choices[index] }}
              </td>
              <td v-else></td>
            </template>
          </template>
          <td>
            <button
              class="delete-btn"
              @click="confirmDelete(question)"
              title="Delete Question"
            >
              🗑️
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="no-questions">
      <b >No questions</b>
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
    v-if="questionToDelete !== undefined"
    @close_request="questionToDelete = undefined"
  >
    <template #body>
      <div class="delete-confirmation">
        <h3>Delete Question</h3>
        <p>Are you sure you want to delete this question?</p>
        <p class="question-text">{{ questionToDelete?.multipleChoicesQuestion.stem }}</p>
        <div class="action-buttons">
          <button class="btn btn-secondary" @click="questionToDelete = undefined">Cancel</button>
          <button class="btn btn-danger" @click="deleteQuestion">Delete</button>
        </div>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import type { Note, PredefinedQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteAddQuestion from "./NoteAddQuestion.vue"
import QuestionManagement from "./QuestionManagement.vue"
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
const questionToDelete = ref<PredefinedQuestion | undefined>()

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
const confirmDelete = (question: PredefinedQuestion) => {
  questionToDelete.value = question
}
const deleteQuestion = async () => {
  if (questionToDelete.value) {
    // await managedApi.restPredefinedQuestionController.deleteQuestion(questionToDelete.value.id)
    questions.value = questions.value.filter(
      (q) => q.id !== questionToDelete.value?.id
    )
    questionToDelete.value = undefined
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
.no-questions {
  margin-top: 10px;
  width: 100%;
  text-align: center;
}
.delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
}
.delete-confirmation {
  text-align: center;
}
.delete-confirmation .question-text {
  font-weight: bold;
  margin: 10px 0;
}
.delete-confirmation .action-buttons {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
}
</style>

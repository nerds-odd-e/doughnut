<template>
  <div>
    <PopButton btn-class="btn btn-primary" title="Add Question">
      <!-- prettier-ignore -->
      <template #default="{ closer }">
        <NoteAddOrEditQuestion
          v-bind="{ note }"
          @close-dialog="
            closer($event);
            questionAdded($event);
          "
        />
      </template>
    </PopButton>
    <table class="question-table mt-2">
      <thead>
        <tr>
          <th>Delete</th>
          <th>Edit</th>
          <th>Approved</th>
          <th>Question Text</th>
          <th v-for="(optionsHeader) in optionsHeaders" :key="optionsHeader">{{ optionsHeader }}</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(question, outerIndex) in questions"
          :key="question.quizQuestion.multipleChoicesQuestion.stem"
        >
          <td>
            <button class="btn btn-danger" title="Delete Question" @click="deleteQuestion(question)">
              Delete Question
            </button>
          </td>
          <td>
            <PopButton btn-class="btn btn-primary" title="Edit Question">
              <!-- prettier-ignore -->
              <template #default="{ closer }">
                <NoteAddOrEditQuestion
                  v-bind="{ note, question }"
                  @close-dialog="
                    closer($event);
                    questionEdited($event);
                  "
                />
              </template>
            </PopButton>
          </td>
          
          <td>
            <input
              :id="'checkbox-' + outerIndex"
              type="checkbox"
              v-model="question.approved"
              @change="toggleApproval(question.id)"
            />
          </td>
          <td>{{ question.quizQuestion.multipleChoicesQuestion.stem }}</td>
          <template
            v-if="question.quizQuestion.multipleChoicesQuestion.choices"
          >
            <td
              v-for="(choice, index) in question.quizQuestion
                .multipleChoicesQuestion.choices"
              :class="{
                'correct-choice': index === question.correctAnswerIndex,
              }"
              :key="index"
            >
              {{ choice }}
            </td>
          </template>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { PropType, onMounted, ref, computed } from "vue"
import { Note, QuizQuestionAndAnswer } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteAddOrEditQuestion from "./NoteAddOrEditQuestion.vue"
import PopButton from "../commons/Popups/PopButton.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})

const questions = ref<QuizQuestionAndAnswer[]>([])
const optionsHeaders = computed(() =>
  questions.value.length === 0
    ? ["A", "B", "C", "D"]
    : new Array(
        Math.max(
          ...questions.value.map(
            (q) => q.quizQuestion.multipleChoicesQuestion.choices.length
          )
        )
      )
        .fill(1)
        .map((_, i) => String.fromCharCode(65 + i))
)
const fetchQuestions = async () => {
  questions.value =
    await managedApi.restQuizQuestionController.getAllQuestionByNote(
      props.note.id
    )
}
const deleteQuestion = async (question: QuizQuestionAndAnswer) => {
  if (question == null) {
    return
  }
  await managedApi.restQuizQuestionController.deleteQuestion(
    props.note.id,
    question.id
  )
  questions.value = questions.value.filter((q) => q.id !== question.id)
}
const questionAdded = (newQuestion: QuizQuestionAndAnswer) => {
  if (newQuestion == null) {
    return
  }
  questions.value.push(newQuestion)
}
const questionEdited = (editedQuestion: QuizQuestionAndAnswer) => {
  if (editedQuestion == null) {
    return
  }
  questions.value = questions.value.map((question) =>
    question.id === editedQuestion.id ? editedQuestion : question
  )
}
const toggleApproval = async (questionId?: number) => {
  if (questionId) {
    await managedApi.restQuizQuestionController.toggleApproval(questionId)
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

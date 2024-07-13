<template>
  <div>
    <PopButton btn-class="btn btn-primary" title="Add Question">
      <!-- prettier-ignore -->
      <template #default="{ closer }">
        <NoteAddQuestion
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
          <th>Approved</th>
          <th>Actions</th>
          <th>Question Text</th>
          <th>A</th>
          <th>B</th>
          <th>C</th>
          <th>D</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(question, outerIndex) in questions"
          :key="question.quizQuestion.multipleChoicesQuestion.stem"
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
            <PopButton btn-class="btn btn-secondary" title="Edit">
              <!-- prettier-ignore -->
              <template #default="{ closer }">
                <NoteAddQuestion
                  v-bind="{ action: action.Edit, note, question }"
                  @close-dialog="
                    closer($event);
                    questionEdited($event);
                  "
                />
              </template>
            </PopButton>
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
import { PropType, onMounted, ref } from "vue"
import { Note, QuizQuestionAndAnswer } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteAddQuestion from "./NoteAddQuestion.vue"
import PopButton from "../commons/Popups/PopButton.vue"
import { ActionQuestionForNote } from "./enum/action.enum"

const { managedApi } = useLoadingApi()
const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})
const action = ActionQuestionForNote
const questions = ref<QuizQuestionAndAnswer[]>([])
const fetchQuestions = async () => {
  questions.value =
    await managedApi.restQuizQuestionController.getAllQuestionByNote(
      props.note.id
    )
}
const questionAdded = (newQuestion: QuizQuestionAndAnswer) => {
  if (newQuestion == null) {
    return
  }
  questions.value.push(newQuestion)
}
const questionEdited = (question: QuizQuestionAndAnswer) => {
  const result = questions.value.find((e) => e.id === question.id)
  if (result) {
    result.correctAnswerIndex = question.correctAnswerIndex
    result.quizQuestion.checkSpell = question.quizQuestion.checkSpell
    result.quizQuestion.imageWithMask = question.quizQuestion.imageWithMask
    result.quizQuestion.multipleChoicesQuestion.stem =
      question.quizQuestion.multipleChoicesQuestion.stem
    result.quizQuestion.multipleChoicesQuestion.choices =
      question.quizQuestion.multipleChoicesQuestion.choices
  }
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

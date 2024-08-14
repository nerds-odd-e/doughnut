<template>
  <div>
    <PopButton btn-class="btn btn-primary" title="Add Question">
      <!-- prettier-ignore -->
      <template #default="{ closer }">
        <NoteAddOrEditQuestion
          v-bind="{ note }"
          @close-dialog="
            closer();
            questionAddedOrEdited($event);
          "
        />
      </template>
    </PopButton>
    <table class="question-table mt-2">
      <thead>
        <tr>
          <th>Actions</th>
          <th>Approved</th>
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
            <button class="btn btn-danger" title="Delete Question" @click=deleteQuestion(question.id)>
              Delete
            </button>
            <PopButton btn-class="btn btn-primary" title="Edit">
              <!-- prettier-ignore -->
              <template #default="{ closer }">
                <NoteAddOrEditQuestion
                  v-bind="{ note, question }"
                  @close-dialog="
                    closer();
                    questionAddedOrEdited($event);
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
import { PropType, onMounted, ref } from "vue"
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
const fetchQuestions = async () => {
  questions.value =
    await managedApi.restQuizQuestionController.getAllQuestionByNote(
      props.note.id
    )
}
const questionAddedOrEdited = (newQuestion: QuizQuestionAndAnswer) => {
  if (newQuestion == null) {
    return
  }

  const index = questions.value.findIndex((q) => {
    return q.id === newQuestion.id
  })
  if (index === -1) {
    questions.value.push(newQuestion)
  } else {
    questions.value.splice(index, 1, newQuestion)
  }
}

const deleteQuestion = async (questionId?: number) => {
  if (questionId) {
    console.log("Delete not implemented yet!")
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

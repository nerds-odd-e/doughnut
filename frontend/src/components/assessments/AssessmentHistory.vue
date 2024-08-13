<template>
  <div> 
    <table class="assessment-table mt-2">
      <thead>
        <tr>
          <th>Notebook</th>
          <th>Attempt At</th>
          <th>Result</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td></td>
          <td></td>
          <td></td>
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

const { managedApi } = useLoadingApi()
const questions = ref<QuizQuestionAndAnswer[]>([])
const questionAdded = (newQuestion: QuizQuestionAndAnswer) => {
  if (newQuestion == null) {
    return
  }
  questions.value.push(newQuestion)
}
const toggleApproval = async (questionId?: number) => {
  if (questionId) {
    await managedApi.restQuizQuestionController.toggleApproval(questionId)
  }
}
onMounted(() => {
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

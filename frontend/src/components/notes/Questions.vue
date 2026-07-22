<template>
  <div>
    <div class="flex gap-2">
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
      <button
        class="daisy-btn daisy-btn-outline"
        @click="showExportDialog = true"
        aria-label="Export question generation request"
        title="Export question generation request for ChatGPT"
      >
        <Upload class="w-6 h-6" />
      </button>
    </div>
    <table class="question-table mt-2" v-if="questions.length">
      <thead>
        <tr>
          <th>Question Text</th>
          <th>A</th>
          <th>B</th>
          <th>C</th>
          <th>D</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="question in questions" :key="question.id">
          <td>
            {{ question.multipleChoicesQuestion.questionStem }}
          </td>
          <template
            v-if="question.multipleChoicesQuestion.responseChoices"
          >
            <td
              v-for="(choice, index) in question
                .multipleChoicesQuestion.responseChoices"
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
              class="daisy-btn daisy-btn-ghost daisy-btn-sm"
              aria-label="Delete question"
              title="Delete question"
              @click="deleteQuestion(question)"
            >
              <Trash2 class="w-5 h-5" />
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="mt-2 w-full text-center">
      <b>No questions</b>
    </div>
  </div>
  <QuestionExportDialog
    v-if="showExportDialog"
    :note-id="note.id"
    @close="showExportDialog = false"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import type { Note, PredefinedQuestion } from "@generated/doughnut-backend-api"
import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteAddQuestion from "./NoteAddQuestion.vue"
import QuestionExportDialog from "./QuestionExportDialog.vue"
import PopButton from "../commons/Popups/PopButton.vue"
import { Trash2, Upload } from "@lucide/vue"

const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})
const questions = ref<PredefinedQuestion[]>([])
const showExportDialog = ref(false)

const fetchQuestions = async () => {
  const { data: allQuestions, error } =
    await PredefinedQuestionController.getAllQuestionByNote({
      path: { note: props.note.id },
    })
  if (!error && allQuestions) {
    questions.value = allQuestions
  }
}
const questionAdded = (newQuestion: PredefinedQuestion) => {
  if (newQuestion == null) {
    return
  }
  questions.value.push(newQuestion)
}
const deleteQuestion = async (question: PredefinedQuestion) => {
  const { error } = await PredefinedQuestionController.deleteQuestion({
    path: { predefinedQuestion: question.id },
  })
  if (!error) {
    questions.value = questions.value.filter((q) => q.id !== question.id)
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

<template>
  <div>
    <div class="daisy-flex daisy-gap-2">
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
        <SvgExport />
      </button>
    </div>
    <table class="question-table daisy-mt-2" v-if="questions.length">
      <thead>
        <tr>
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
          :key="question.multipleChoicesQuestion.f0__stem"
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
              {{ question.multipleChoicesQuestion.f0__stem }}
            </span>
          </td>
          <template
            v-if="question.multipleChoicesQuestion.f1__choices"
          >
            <td
              v-for="(choice, index) in question
                .multipleChoicesQuestion.f1__choices"
              :class="{
                'correct-choice': index === question.correctAnswerIndex,
              }"
              :key="`${question.id}-${choice}`"
            >
              {{ choice }}
            </td>
          </template>
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
  <QuestionExportDialog
    v-if="showExportDialog"
    :note-id="note.id"
    @close="showExportDialog = false"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import type { Note, PredefinedQuestion } from "@generated/backend"
import {
  getAllQuestionByNote,
  toggleApproval as toggleApprovalApi,
} from "@generated/backend/sdk.gen"
import NoteAddQuestion from "./NoteAddQuestion.vue"
import QuestionManagement from "./QuestionManagement.vue"
import QuestionExportDialog from "./QuestionExportDialog.vue"
import PopButton from "../commons/Popups/PopButton.vue"
import SvgExport from "../svgs/SvgExport.vue"

const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})
const questions = ref<PredefinedQuestion[]>([])
const openedQuestion = ref<PredefinedQuestion | undefined>()
const showExportDialog = ref(false)

const fetchQuestions = async () => {
  const { data: allQuestions, error } = await getAllQuestionByNote({
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
const toggleApproval = async (questionId?: number) => {
  if (questionId) {
    await toggleApprovalApi({
      path: { predefinedQuestion: questionId },
    })
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

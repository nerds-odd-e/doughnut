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
          <th>Actions</th>
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
              :key="index"
            >
              {{ choice }}
            </td>
          </template>
          <td>
            <div class="daisy-flex daisy-gap-2">
              <button
                class="daisy-btn daisy-btn-ghost daisy-btn-sm"
                title="Edit question"
                @click="openedQuestion = question"
              >
                <SvgEdit />
              </button>
              <button
                class="daisy-btn daisy-btn-ghost daisy-btn-sm"
                title="Delete question"
                @click="deleteQuestion(question)"
              >
                <SvgRemove />
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
        @question-updated="handleQuestionUpdated"
        @close-dialog="openedQuestion = undefined"
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
import { PredefinedQuestionController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import NoteAddQuestion from "./NoteAddQuestion.vue"
import QuestionManagement from "./QuestionManagement.vue"
import QuestionExportDialog from "./QuestionExportDialog.vue"
import PopButton from "../commons/Popups/PopButton.vue"
import SvgExport from "../svgs/SvgExport.vue"
import SvgEdit from "../svgs/SvgEdit.vue"
import SvgRemove from "../svgs/SvgRemove.vue"
import usePopups from "../commons/Popups/usePopups"

const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})
const questions = ref<PredefinedQuestion[]>([])
const openedQuestion = ref<PredefinedQuestion | undefined>()
const showExportDialog = ref(false)
const { popups } = usePopups()

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
const toggleApproval = async (questionId?: number) => {
  if (questionId) {
    await apiCallWithLoading(() =>
      PredefinedQuestionController.toggleApproval({
        path: { predefinedQuestion: questionId },
      })
    )
  }
}
const deleteQuestion = async (question: PredefinedQuestion) => {
  if (
    await popups.confirm(
      `Are you sure you want to delete this question: "${question.multipleChoicesQuestion.f0__stem}"?`
    )
  ) {
    const { error } = await apiCallWithLoading(() =>
      PredefinedQuestionController.deleteQuestion({
        path: { predefinedQuestion: question.id! },
      })
    )
    if (!error) {
      questions.value = questions.value.filter((q) => q.id !== question.id)
    }
  }
}
const handleQuestionUpdated = (updatedQuestion: PredefinedQuestion) => {
  const index = questions.value.findIndex((q) => q.id === updatedQuestion.id)
  if (index !== -1) {
    questions.value[index] = updatedQuestion
  }
  openedQuestion.value = undefined
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

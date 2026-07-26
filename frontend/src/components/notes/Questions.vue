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
        type="button"
        class="daisy-btn daisy-btn-error"
        title="Delete Question"
        aria-label="Delete Question"
        :disabled="selectedQuestionIds.length === 0"
        @click="showDeleteModal = true"
      >
        Delete Question
      </button>
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
          <th>
            <input
              type="checkbox"
              class="daisy-checkbox"
              aria-label="Select all questions"
              :checked="allSelected"
              @change="toggleSelectAll"
            />
          </th>
          <th>Question Text</th>
          <th>A</th>
          <th>B</th>
          <th>C</th>
          <th>D</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="question in questions"
          :key="question.id"
        >
          <td>
            <input
              type="checkbox"
              class="daisy-checkbox"
              :aria-label="`Select question ${question.multipleChoicesQuestion.questionStem}`"
              :checked="selectedQuestionIds.includes(question.id!)"
              @change="toggleQuestion(question.id!)"
            />
          </td>
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
  <dialog
    ref="deleteDialogRef"
    class="daisy-modal"
    :class="{ 'daisy-modal-open': showDeleteModal }"
    @close="showDeleteModal = false"
  >
    <div class="daisy-modal-box">
      <h3 class="font-bold text-lg">Confirm deletion</h3>
      <p class="py-2">These questions will be deleted:</p>
      <ul class="list-disc pl-5 py-2">
        <li v-for="stem in selectedQuestionStems" :key="stem">
          {{ stem }}
        </li>
      </ul>
      <div class="daisy-modal-action">
        <button
          type="button"
          class="daisy-btn"
          aria-label="Cancel"
          @click="showDeleteModal = false"
        >
          Cancel
        </button>
        <button
          type="button"
          class="daisy-btn daisy-btn-error"
          aria-label="Confirm"
          @click="confirmDelete"
        >
          Confirm
        </button>
      </div>
    </div>
    <form method="dialog" class="daisy-modal-backdrop">
      <button type="submit" @click="showDeleteModal = false">close</button>
    </form>
  </dialog>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, onMounted, ref } from "vue"
import type { Note, PredefinedQuestion } from "@generated/doughnut-backend-api"
import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteAddQuestion from "./NoteAddQuestion.vue"
import QuestionExportDialog from "./QuestionExportDialog.vue"
import PopButton from "../commons/Popups/PopButton.vue"
import { Upload } from "@lucide/vue"
import { useDaisyDialog } from "@/composables/useDaisyDialog"
import { useToast } from "@/composables/useToast"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})
const questions = ref<PredefinedQuestion[]>([])
const selectedQuestionIds = ref<number[]>([])
const showExportDialog = ref(false)
const showDeleteModal = ref(false)
const deleteDialogRef = ref<HTMLDialogElement | null>(null)
useDaisyDialog(showDeleteModal, deleteDialogRef)
const { showSuccessToast } = useToast()

const allSelected = computed(
  () =>
    questions.value.length > 0 &&
    selectedQuestionIds.value.length === questions.value.length
)

const selectedQuestionStems = computed(() =>
  questions.value
    .filter((q) => q.id != null && selectedQuestionIds.value.includes(q.id))
    .map((q) => q.multipleChoicesQuestion.questionStem)
)

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

const toggleQuestion = (id: number) => {
  if (selectedQuestionIds.value.includes(id)) {
    selectedQuestionIds.value = selectedQuestionIds.value.filter(
      (selectedId) => selectedId !== id
    )
  } else {
    selectedQuestionIds.value = [...selectedQuestionIds.value, id]
  }
}

const toggleSelectAll = (event: Event) => {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    selectedQuestionIds.value = questions.value
      .map((q) => q.id)
      .filter((id): id is number => id !== undefined)
  } else {
    selectedQuestionIds.value = []
  }
}

const confirmDelete = async () => {
  const ids = [...selectedQuestionIds.value]
  const { error } = await apiCallWithLoading(() =>
    PredefinedQuestionController.deleteQuestions({
      path: { note: props.note.id },
      body: ids,
    })
  )
  if (error) {
    return
  }
  questions.value = questions.value.filter(
    (q) => q.id == null || !ids.includes(q.id)
  )
  selectedQuestionIds.value = []
  showDeleteModal.value = false
  showSuccessToast("Delete success")
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

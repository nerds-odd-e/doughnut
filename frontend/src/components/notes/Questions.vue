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
    <PopButton btn-class="daisy-btn daisy-btn-primary" title="Delete Question">
      <template #default="{ closer }">
        <NoteDeleteQuestion
          :questions="questionStrings" 
          @close-dialog="
            closer();
            questionDeleted($event);
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
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref, computed } from "vue"
import type { Note, PredefinedQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteAddQuestion from "./NoteAddQuestion.vue"
import NoteDeleteQuestion from "./NoteDeleteQuestion.vue"
import QuestionManagement from "./QuestionManagement.vue"
import PopButton from "../commons/Popups/PopButton.vue"

const { managedApi } = useLoadingApi()
const props = defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
})

const questionStrings = computed(() => {
  return questions.value.map(q => q.multipleChoicesQuestion.stem)
})
const questions = ref<PredefinedQuestion[]>([])
const openedQuestion = ref<PredefinedQuestion | undefined>()

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
const questionDeleted = (deletedQuestion: string[]) => {
  if (!Array.isArray(deletedQuestion) || deletedQuestion.length === 0) {
    return;
  }
  
  questions.value = questions.value.filter(
    (q) => !deletedQuestion.includes(q.multipleChoicesQuestion.stem)
  );
}
const toggleApproval = async (questionId?: number) => {
  if (questionId) {
    await managedApi.restPredefinedQuestionController.toggleApproval(questionId)
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

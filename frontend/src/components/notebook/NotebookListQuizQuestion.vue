<template>
  <div class="container px-4 pb-4">
    <table class="question-table mt-2" title="view all note questions">
      <thead>
        <tr>
          <th>Approved</th>
          <th>Note</th>
          <th>Question Text</th>
          <th>A</th>
          <th>B</th>
          <th>C</th>
          <th>D</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(noteQuestion, idx) of noteAndQuestions" :key="idx">
          <td>
            <input
              :id="'checkbox-' + idx"
              type="checkbox"
              v-model="noteQuestion.quizQuestion.approved"
              @change="toggleApproval(noteQuestion.quizQuestion.id)"
            />
          </td>
          <td>
            {{ noteQuestion.topicConstructor }}
          </td>
          <td>
            {{
              noteQuestion.quizQuestion.quizQuestion.multipleChoicesQuestion
                .stem
            }}
          </td>
          <td
            v-for="(choice, choiceIndex) of noteQuestion.quizQuestion
              .quizQuestion.multipleChoicesQuestion.choices"
            :key="choiceIndex"
            :class="{
              'correct-choice':
                choiceIndex === noteQuestion.quizQuestion.correctAnswerIndex,
            }"
          >
            {{ choice }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { Note } from "@/generated/backend/models/Note"
import { QuizQuestionAndAnswer } from "@/generated/backend"

const restApi = useLoadingApi()
const notes = ref<Note[]>([])

interface Props {
  notebookId: number
}

interface NoteAndQuestion {
  topicId: number
  topicConstructor: string
  quizQuestion: QuizQuestionAndAnswer
}

const props = defineProps<Props>()

const noteAndQuestions = computed<NoteAndQuestion[]>(() => {
  return notes.value.reduce((acc: NoteAndQuestion[], item: Note) => {
    const questionsWithName: NoteAndQuestion[] =
      item.quizQuestionAndAnswers!.map((question) => {
        return {
          topicId: item.noteTopic.id,
          topicConstructor: item.noteTopic.topicConstructor,
          quizQuestion: question,
        } as NoteAndQuestion
      })
    return acc.concat(questionsWithName)
  }, [])
})

const getNoteQuestions = (notebookId: number) => {
  return restApi.managedApi.restNotebookController.getNotes(notebookId)
}

const toggleApproval = async (questionId?: number) => {
  if (questionId) {
    await restApi.managedApi.restQuizQuestionController.toggleApproval(
      questionId
    )
  }
}

onMounted(async () => {
  notes.value = await getNoteQuestions(props.notebookId)
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

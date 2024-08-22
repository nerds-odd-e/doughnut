<template>
  <div class="notebook-questions-list">
    <div v-for="note in notes">
      <h2>{{note.noteTopic.topicConstructor}}</h2>
      <h3 v-if="!questionsByNote[note.id]?.length">No questions</h3>
      <ul>
        <li v-for="question in questionsByNote[note.id]">{{ question.quizQuestion.multipleChoicesQuestion.stem }}</li>
      </ul>
    </div>
  </div>
</template>
<script setup lang="ts">
import { onMounted, PropType, ref } from "vue"
import { Note, Notebook, QuizQuestionAndAnswer } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
const props = defineProps({
  notebook: {
    type: Object as PropType<Notebook>,
    required: true,
  },
})
const managedApi = useLoadingApi().managedApi
const notes = ref<Note[] | undefined>(undefined)
const questionsByNote = ref<Record<number, QuizQuestionAndAnswer[]>>({})
const fetchData = async () => {
  notes.value = await managedApi.restNotebookController.getNotes(
    props.notebook.id
  )
  notes.value.forEach(async (note) => {
    questionsByNote.value[note.id] =
      await managedApi.restQuizQuestionController.getAllQuestionByNote(note.id)
  })
}
onMounted(() => {
  fetchData()
})
</script>
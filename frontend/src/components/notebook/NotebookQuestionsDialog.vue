<template>
  <div class="notebook-questions-list">
    <div v-for="note in notes">
      <h3>{{note.noteTopology.titleOrPredicate}}</h3>
      <Questions :note="note" />
    </div>
  </div>
</template>
<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import type { Note, Notebook } from "@generated/backend"
import Questions from "../notes/Questions.vue"
import { NotebookController } from "@generated/backend/sdk.gen"
const props = defineProps({
  notebook: {
    type: Object as PropType<Notebook>,
    required: true,
  },
})
const notes = ref<Note[] | undefined>(undefined)
const fetchData = async () => {
  const { data: notesList, error } = await NotebookController.getNotes({
    path: { notebook: props.notebook.id },
  })
  if (!error) {
    notes.value = notesList!
  }
}
onMounted(() => {
  fetchData()
})
</script>
<style scoped>
.notebook-questions-list > div:not(:last-child) {
    border-bottom: 1px solid grey;
    padding-bottom: 10px;
    margin-bottom: 10px;
}
</style>

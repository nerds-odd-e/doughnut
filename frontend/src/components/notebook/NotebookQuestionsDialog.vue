<template>
  <div class="notebook-questions-list">
    <div v-for="note in notes">
      <h3>{{note.noteTopic.topicConstructor}}</h3>
      <Questions :note="note" />
    </div>
  </div>
</template>
<script setup lang="ts">
import { onMounted, PropType, ref } from "vue"
import { Note, Notebook } from "@/generated/backend"
import Questions from "../notes/Questions.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
const props = defineProps({
  notebook: {
    type: Object as PropType<Notebook>,
    required: true,
  },
})
const managedApi = useLoadingApi().managedApi
const notes = ref<Note[] | undefined>(undefined)
const fetchData = async () => {
  notes.value = await managedApi.restNotebookController.getNotes(
    props.notebook.id
  )
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
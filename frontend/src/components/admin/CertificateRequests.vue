<template>
  <table class="table" v-if="notebooks?.length">
    <thead>
    <tr>
      <th>Notebook</th>
      <th>Username</th>
    </tr>
    </thead>
    <tbody>
    <tr v-for="notebook in notebooks" :key="notebook.id">
      <td>
        <NoteTopicWithLink v-bind="{ noteTopic: notebook.headNote.noteTopic }" />
      </td>
      <td>
        {{ notebook.creatorId }}
      </td>
      <td>
        <button
          class="btn btn-primary"
          style="background-color: green; border-color: green;"
          @click="approveNoteBook(notebook.id)"
        >
        Approve
        </button>
      </td>
    </tr>
    </tbody>
  </table>
  <div v-else>
    No certification request found.
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { Notebook } from "@/generated/backend"
import NoteTopicWithLink from "@/components/notes/NoteTopicWithLink.vue"

const { managedApi } = useLoadingApi()

const notebooks = ref<Notebook[] | undefined>(undefined)

const fetchNotebooks = async () => {
  notebooks.value =
    await managedApi.restNotebookController.getAllPendingRequestNotebooks()
}

const approveNoteBook = async (notebookId: number) => {
  await managedApi.restNotebookController.approveNoteBook(notebookId)
  fetchNotebooks()
}
onMounted(() => {
  fetchNotebooks()
})
</script>

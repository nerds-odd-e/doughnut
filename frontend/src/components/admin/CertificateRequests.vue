<template>
  <table class="table" v-if="notebooks?.length">
    <thead>
    <tr>
      <th>Notebook</th>
      <th>Username</th>
      <th>Status</th>
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
          class="btn btn-dange"
          @click="approveNoteBook(notebook)"
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

const fetchData = async () => {
  notebooks.value =
    await managedApi.restNotebookController.getAllPendingRequestNotebooks()
}

const approveNoteBook = async (notebook: Notebook) => {
  await managedApi.restNotebookController.approveNoteBook(notebook.id)
}
onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="recently-added-notes">
    <ContentLoader v-if="!notes" />
    <table v-else class="table">
      <thead>
        <tr>
          <th>Note</th>
          <th>Notebook</th>
          <th>Created</th>
          <th>Updated</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="note in notes" :key="note.id">
          <td>
            <NoteTitleWithLink :noteTopology="note.note.noteTopology" />
          </td>
          <td>
            <NotebookLink v-if="note.notebook" :notebook="note.notebook" />
          </td>
          <td>{{ new Date(note.note.createdAt).toLocaleString() }}</td>
          <td>{{ new Date(note.note.updatedAt).toLocaleString() }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { NoteRealm } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import { globalClientSilent } from "@/managedApi/clientSetup"
import NoteTitleWithLink from "@/components/notes/NoteTitleWithLink.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import NotebookLink from "@/components/notes/NotebookLink.vue"

const notes = ref<NoteRealm[] | undefined>(undefined)

const fetchData = async () => {
  const { data: recentNotes, error } = await NoteController.getRecentNotes({
    client: globalClientSilent,
  })
  if (!error) {
    notes.value = recentNotes!
  }
}

onMounted(() => {
  fetchData()
})
</script>

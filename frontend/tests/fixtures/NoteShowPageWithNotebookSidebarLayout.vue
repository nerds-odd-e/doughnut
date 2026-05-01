<template>
  <NotebookSidebarLayout>
    <NoteShowPage :note-id="noteId" />
  </NotebookSidebarLayout>
</template>

<script setup lang="ts">
import NotebookSidebarLayout from "@/layouts/NotebookSidebarLayout.vue"
import NoteShowPage from "@/pages/NoteShowPage.vue"

defineProps({
  noteId: { type: Number, required: false },
})
</script>

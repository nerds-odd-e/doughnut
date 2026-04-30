<template>
  <NotebookSidebarLayout>
    <NoteShowPage
      :slug="slug"
      :notebook-id="notebookId"
      :note-slug-path="noteSlugPath"
    />
  </NotebookSidebarLayout>
</template>

<script setup lang="ts">
import NotebookSidebarLayout from "@/layouts/NotebookSidebarLayout.vue"
import NoteShowPage from "@/pages/NoteShowPage.vue"

defineProps({
  slug: { type: String, required: false },
  notebookId: { type: Number, required: false },
  noteSlugPath: { type: String, required: false },
})
</script>

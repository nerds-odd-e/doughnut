<template>
  <Sidebar
    :active-note-realm="noteRealm"
    :notebook-id="notebookId"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import Sidebar from "./Sidebar.vue"
import type { NoteRealm } from "@generated/doughnut-backend-api"

defineProps({
  noteRealm: {
    type: Object as PropType<NoteRealm | undefined>,
    required: false,
  },
  notebookId: { type: Number, required: true },
})
</script>

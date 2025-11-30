<template>
  <Sidebar
    v-bind="{
      activeNoteRealm: noteRealm,
    }"
  />
</template>

<script setup lang="ts">
import { type PropType } from "vue"
import Sidebar from "./Sidebar.vue"
import type { NoteRealm } from "@generated/backend"

defineProps({
  noteRealm: { type: Object as PropType<NoteRealm>, required: true },
})
</script>

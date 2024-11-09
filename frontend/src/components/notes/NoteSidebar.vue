<template>
  <Sidebar
    v-bind="{
      activeNoteRealm: noteRealm,
      storageAccessor,
    }"
  />
</template>

<script setup lang="ts">
import { type PropType } from "vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import Sidebar from "./Sidebar.vue"
import type { NoteRealm } from "@/generated/backend"

defineProps({
  noteRealm: { type: Object as PropType<NoteRealm>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>

<template>
  <Sidebar
    v-bind="{
      activeNoteRealm: noteRealm,
      storageAccessor,
    }"
  />
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import Sidebar from "./Sidebar.vue"

const props = defineProps({
  noteId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const noteRealm = computed(
  () => props.storageAccessor.refOfNoteRealm(props.noteId).value
)
</script>

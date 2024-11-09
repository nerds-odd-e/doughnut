<template>
  <SidebarInner
    :class="{ 'is-disabled': !activeNoteRealm }"
    v-if="activeNoteRealm && headNoteId"
    v-bind="{
      noteId: headNoteId,
      activeNoteRealm: activeNoteRealm,
      storageAccessor,
    }"
    :key="headNoteId"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed } from "vue"
import type { NoteRealm } from "@/generated/backend"
import type { StorageAccessor } from "../../store/createNoteStorage"
import SidebarInner from "./SidebarInner.vue"

const props = defineProps({
  activeNoteRealm: { type: Object as PropType<NoteRealm> },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const headNoteId = computed(() => {
  if (!props.activeNoteRealm) return undefined
  let cursor = props.activeNoteRealm.note.noteTopic
  while (cursor.parentNoteTopic) {
    cursor = cursor.parentNoteTopic
  }
  return cursor.id
})
</script>

<style scoped>
.is-disabled {
  opacity: 0.5;
  pointer-events: none;
}
</style>

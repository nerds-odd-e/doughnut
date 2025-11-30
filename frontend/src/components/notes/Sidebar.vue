<template>
  <div class="daisy-ml-[-1rem]">
    <SidebarInner
      :class="{ 'is-disabled': !activeNoteRealm }"
      v-if="activeNoteRealm && headNoteId"
      v-bind="{
        noteId: headNoteId,
        activeNoteRealm: activeNoteRealm,
      }"
      :key="headNoteId"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed } from "vue"
import type { NoteRealm } from "@generated/backend"
import SidebarInner from "./SidebarInner.vue"

const props = defineProps({
  activeNoteRealm: { type: Object as PropType<NoteRealm> },
})

const headNoteId = computed(() => {
  if (!props.activeNoteRealm) return undefined
  let cursor = props.activeNoteRealm.note.noteTopology
  while (cursor.parentOrSubjectNoteTopology) {
    cursor = cursor.parentOrSubjectNoteTopology
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

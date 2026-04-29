<template>
  <div class="daisy-ml-[-1rem]">
    <NoteSidebarToolbar
      v-if="!sidebarReadonly"
      :notebook-id="notebookId"
      :note="activeNoteRealm?.note"
      :topology-head-resolved="topologyHeadResolved"
    />
    <SidebarInner
      :class="{ 'is-disabled': !activeNoteRealm }"
      v-if="activeNoteRealm && topologyHeadResolved"
      v-bind="{
        noteId: topologyHeadNoteId!,
        activeNoteRealm: activeNoteRealm,
      }"
      :key="topologyHeadNoteId"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType, Ref } from "vue"
import { computed, inject } from "vue"
import type { NoteRealm, User } from "@generated/doughnut-backend-api"
import NoteSidebarToolbar from "./NoteSidebarToolbar.vue"
import SidebarInner from "./SidebarInner.vue"

const props = defineProps({
  /** When set with a loaded realm — sidebar listing for this notebook tree */
  activeNoteRealm: { type: Object as PropType<NoteRealm>, required: false },
  /** Opens root-note POST /api/notebooks/{id}/create-note whenever topology head is missing */
  notebookId: { type: Number, required: true },
})

const currentUser = inject<Ref<User | undefined>>("currentUser")
const sidebarReadonly = computed(
  () =>
    !currentUser?.value ||
    (props.activeNoteRealm != null && props.activeNoteRealm.fromBazaar === true)
)

const topologyHeadNoteId = computed(() => {
  if (!props.activeNoteRealm?.note?.noteTopology) return undefined
  let cursor = props.activeNoteRealm.note.noteTopology
  while (cursor?.parentOrSubjectNoteTopology != null) {
    cursor = cursor.parentOrSubjectNoteTopology
  }
  return cursor?.id
})

const topologyHeadResolved = computed(() => topologyHeadNoteId.value != null)
</script>

<style scoped>
.is-disabled {
  opacity: 0.5;
  pointer-events: none;
}
</style>

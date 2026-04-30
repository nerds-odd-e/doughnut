<template>
  <div class="daisy-ml-[-1rem]">
    <NoteSidebarToolbar
      v-if="!sidebarReadonly"
      :notebook-id="notebookId"
      :note="activeNoteRealm?.note"
      :topology-head-resolved="noteContextResolved"
    />
    <SidebarInner
      v-if="sidebarTreeShown"
      :key="notebookId"
      :notebook-id="notebookId"
      :active-note-realm="activeNoteRealm"
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
  /** When set, highlights the active note and expands its ancestors */
  activeNoteRealm: {
    type: Object as PropType<NoteRealm | undefined>,
    required: false,
  },
  /** Opens root-note POST /api/notebooks/{id}/create-note whenever topology head is missing */
  notebookId: { type: Number, required: true },
})

const currentUser = inject<Ref<User | undefined>>("currentUser")
const sidebarReadonly = computed(
  () =>
    !currentUser?.value ||
    (props.activeNoteRealm != null && props.activeNoteRealm.fromBazaar === true)
)

const noteContextResolved = computed(
  () => props.activeNoteRealm?.note?.noteTopology != null
)

/** Notebook overview pages may load root notes without an anchor note (e.g. no `index` slug). */
const sidebarTreeShown = computed(
  () =>
    props.activeNoteRealm === undefined ||
    props.activeNoteRealm.note.noteTopology != null
)
</script>

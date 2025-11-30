<template>
  <div class="daisy-alert daisy-alert-warning" v-if="note.deletedAt">
    This note has been deleted
  </div>
  <NoteTitleAsPredicate
    v-if="note.noteTopology.objectNoteTopology"
    v-bind="{ noteTopology: note.noteTopology, readonly }"
  />
  <NoteEditableTitle
    v-else
    v-bind="{ noteTopology: note.noteTopology, readonly }"
  />
  <div role="details" class="note-details">
    <NoteEditableDetails
      v-bind="{ readonly, noteId: note.id, noteDetails: note.details, asMarkdown }"
    />
  </div>
</template>

<script setup lang="ts">
import { type PropType } from "vue"
import type { Note } from "@generated/backend"
import NoteEditableTitle from "./NoteEditableTitle.vue"
import NoteTitleAsPredicate from "./NoteTitleAsPredicate.vue"
import NoteEditableDetails from "./NoteEditableDetails.vue"

defineProps({
  note: { type: Object as PropType<Note>, required: true },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
})
</script>

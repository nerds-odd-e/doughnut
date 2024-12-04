<template>
  <div class="alert alert-warning" v-if="note.deletedAt">
    This note has been deleted
  </div>
  <NoteLinkTopic
    v-if="note.noteTopology.objectNoteTopology"
    v-bind="{ noteTopology: note.noteTopology, readonly, storageAccessor }"
  />
  <NoteEditableTopic
    v-else
    v-bind="{ noteTopology: note.noteTopology, readonly, storageAccessor }"
  />
  <div role="details" class="note-details">
    <NoteEditableDetails
      v-bind="{ readonly, noteId: note.id, noteDetails: note.details, storageAccessor, asMarkdown }"
    />
  </div>
</template>

<script setup lang="ts">
import { type PropType } from "vue"
import type { Note } from "@/generated/backend"
import type { StorageAccessor } from "../../../store/createNoteStorage"
import NoteEditableTopic from "./NoteEditableTopic.vue"
import NoteLinkTopic from "./NoteLinkTopic.vue"
import NoteEditableDetails from "./NoteEditableDetails.vue"

defineProps({
  note: { type: Object as PropType<Note>, required: true },
  readonly: { type: Boolean, default: true },
  asMarkdown: Boolean,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>

<style scoped>
.note-topic {
  margin-top: 0px;
  padding-top: 10px;
  color: black;
}

.outdated-label {
  display: inline-block;
  height: 100%;
  vertical-align: middle;
  margin-left: 20px;
  padding-bottom: 10px;
  color: red;
}
</style>

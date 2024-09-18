<template>
  <h3 v-if="note">
    Link
    <strong
      ><NoteTopicComponent v-bind="{ noteTopic: note.noteTopic }"
    /></strong>
    to
  </h3>
  <h3 v-else>Searching</h3>
  <SearchNote
    v-if="!targetNoteTopic"
    v-bind="{ noteId: note?.id }"
    @selected="targetNoteTopic = $event"
  />
  <LinkNoteFinalize
    v-if="targetNoteTopic && note"
    v-bind="{ targetNoteTopic, note, storageAccessor }"
    @success="$emit('closeDialog')"
    @go-back="targetNoteTopic = undefined"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { Note } from "@/generated/backend"
import { NoteTopic } from "@/generated/backend"
import LinkNoteFinalize from "./LinkNoteFinalize.vue"
import NoteTopicComponent from "../notes/core/NoteTopicComponent.vue"
import SearchNote from "../search/SearchNote.vue"
import type { StorageAccessor } from "../../store/createNoteStorage"

defineProps({
  note: Object as PropType<Note>,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
defineEmits(["closeDialog"])

const targetNoteTopic = ref<NoteTopic | undefined>(undefined)
</script>

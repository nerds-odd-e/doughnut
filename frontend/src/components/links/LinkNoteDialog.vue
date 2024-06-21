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
import { Note, NoteTopic } from "@/generated/backend"
import { PropType, ref } from "vue"
import { StorageAccessor } from "../../store/createNoteStorage"

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

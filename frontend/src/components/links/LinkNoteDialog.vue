<template>
  <h3 v-if="note">
    Link
    <strong><NoteTopic v-bind="{ noteTopic: note.noteTopic }" /></strong> to
  </h3>
  <h3 v-else>Searching</h3>
  <SearchNote
    v-if="!targetNote"
    v-bind="{ noteId: note?.id }"
    @selected="targetNote = $event"
  />
  <LinkNoteFinalize
    v-if="targetNote && note"
    v-bind="{ targetNote, note, storageAccessor }"
    @success="$emit('closeDialog')"
    @go-back="targetNote = undefined"
  />
</template>

<script setup lang="ts">
import { PropType, ref } from "vue";
import { Note } from "@/generated/backend";
import LinkNoteFinalize from "./LinkNoteFinalize.vue";
import NoteTopic from "../notes/core/NoteTopic.vue";
import SearchNote from "../search/SearchNote.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

defineProps({
  note: Object as PropType<Note>,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});
defineEmits(["closeDialog"]);

const targetNote = ref<Note | undefined>(undefined);
</script>

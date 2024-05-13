<template>
  <NoteEditableTopic
    :note-id="note.id"
    :note-topic-constructor="note.topicConstructor"
    :note-topic="note.topic"
    :storage-accessor="storageAccessor"
  />
  <div role="details" class="note-details">
    <NoteEditableDetails
      :note-id="note.id"
      :note-details="note.details"
      :storage-accessor="storageAccessor"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import { type StorageAccessor } from "../../../store/createNoteStorage";
import NoteEditableTopic from "./NoteEditableTopic.vue";
import NoteEditableDetails from "./NoteEditableDetails.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    NoteEditableTopic,
    NoteEditableDetails,
  },
});
</script>
<style scoped>
.download-btn {
  text-decoration: underline;
}
</style>

<template>
  <div style="display: flex">
    <NoteEditableTopic
      :note-id="note.id"
      :note-topic-constructor="note.topicConstructor"
      :note-topic="note.topic"
      :storage-accessor="storageAccessor"
    />
    <slot name="topic-additional" />
  </div>
  <div role="details" class="note-content">
    <NoteEditableDetails
      :note-id="note.id"
      :note-details="note.details"
      :storage-accessor="storageAccessor"
    />
    <slot name="note-content-other" />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { type StorageAccessor } from "../../store/createNoteStorage";
import NoteEditableTopic from "./NoteEditableTopic.vue";
import NoteEditableDetails from "./NoteEditableDetails.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
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

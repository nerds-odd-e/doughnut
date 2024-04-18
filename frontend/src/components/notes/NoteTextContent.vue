<template>
  <div style="display: flex">
    <NoteEditableTopic
      :note-id="note.id"
      :note-topic-constructor="note.topicConstructor"
      :note-topic="note.topic"
      :storage-accessor="storageAccessor"
    />
    <slot name="topic-additional" />
    <button
      @click="downloadAudioFile(note.noteAccessories.audioId!)"
      v-if="note.noteAccessories.audioName && isTesting"
    >
      Download {{ note.noteAccessories.audioName }}
    </button>
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
import { Note } from "@/generated/backend";
import getEnvironment from "@/managedApi/window/getEnvironment";
import { type StorageAccessor } from "../../store/createNoteStorage";
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
  computed: {
    isTesting() {
      return getEnvironment() === "testing";
    },
  },
  methods: {
    async downloadAudioFile(audioId: number) {
      await this.storageAccessor.storedApi().downloadAudio(audioId);
    },
  },
});
</script>

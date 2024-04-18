<template>
  <form @submit.prevent.once="processForm">
    <NoteUploadAudioForm
      v-if="!!formData"
      v-model="formData"
      :errors="noteFormErrors"
    />
    <input type="submit" value="Save Only" class="btn btn-primary" />
    <input type="submit" value="Save and Convert" class="btn btn-primary" />
    <input
      type="submit"
      value="Convert Only"
      class="btn btn-primary"
      @click="convertToSRT"
    />
  </form>
  <textarea :value="srt"></textarea>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import { StorageAccessor } from "../../store/createNoteStorage";
import NoteUploadAudioForm from "./NoteUploadAudioForm.vue";

export default defineComponent({
  components: {
    NoteUploadAudioForm,
  },
  props: {
    note: { type: Object as PropType<Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["closeDialog"],
  data() {
    return {
      formData: {} as { file: Blob },
      noteFormErrors: {},
      srt: "",
    };
  },

  methods: {
    processForm() {
      this.storageAccessor
        .storedApi()
        .uploadAudio(this.note.id, this.formData)
        .then(() => this.$emit("closeDialog"))
        .catch((error) => {
          this.noteFormErrors = error;
        });
    },
    convertToSRT() {
      this.srt = "1\n00:00:00,000 --> 00:00:02,000\nHello, this is a test.";
    },
  },
});
</script>

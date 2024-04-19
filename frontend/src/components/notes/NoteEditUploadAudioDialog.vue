<template>
  <form @submit.prevent.once="uploadAudio">
    <NoteUploadAudioForm
      v-if="!!formData"
      v-model="formData"
      :errors="noteFormErrors"
    />
    <input type="submit" value="Save" class="btn btn-primary" />
    <input
      value="Save and Convert to SRT"
      class="btn btn-primary"
      @click="convertToSRT"
    />
    <input
      value="Convert to SRT"
      class="btn btn-primary"
      @click="convertToSRT"
    />
  </form>
  <textarea :value="srt"></textarea>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note, AudioUploadDTO } from "@/generated/backend";
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
      formData: {} as AudioUploadDTO,
      noteFormErrors: {},
      srt: "",
    };
  },

  methods: {
    uploadAudio() {
      this.storageAccessor
        .storedApi()
        .uploadAudio(this.note.id, this.formData)
        .then(() => this.$emit("closeDialog"))
        .catch((error) => {
          this.noteFormErrors = {uploadAudioFile: error.body.message ?? "Unexpected error occured"};
        });
    },
    convertToSRT() {
      this.storageAccessor
        .storedApi()
        .convertAudio(true, this.formData)
        .then(
          () =>
            (this.srt =
              "1\n00:00:00,000 --> 00:00:02,000\nHello, this is a test."),
        )
        .catch((error) => {
          this.noteFormErrors = error;
        });
    },
  },
});
</script>

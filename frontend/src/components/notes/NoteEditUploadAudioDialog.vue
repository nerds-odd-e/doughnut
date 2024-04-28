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
      @click="uploadAudioAndConvertToSRT"
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
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "../../store/createNoteStorage";
import NoteUploadAudioForm from "./NoteUploadAudioForm.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
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
    async uploadAudio() {
      try {
        await this.storageAccessor
          .storedApi()
          .uploadAudio(this.note.id, this.formData, false);
        this.$emit("closeDialog");
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } catch (error: any) {
        this.noteFormErrors = {
          uploadAudioFile: error.body.message ?? "Unexpected error occured",
        };
      }
    },
    async uploadAudioAndConvertToSRT() {
      try {
        await this.storageAccessor
          .storedApi()
          .uploadAudio(this.note.id, this.formData, true);
        this.srt =
          await this.managedApi.restAiAudioController.convertAudioToSrt(
            this.note.id,
          );

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } catch (error: any) {
        this.noteFormErrors = {
          uploadAudioFile: error.body.message ?? "Unexpected error occured",
        };
      }
    },
    async convertToSRT() {
      try {
        const response = await this.storageAccessor
          .storedApi()
          .convertAudio(this.formData);
        this.srt = response;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } catch (error: any) {
        this.noteFormErrors = error;
      }
    },
  },
});
</script>

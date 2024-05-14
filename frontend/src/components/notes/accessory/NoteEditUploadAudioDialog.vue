<template>
  <form @submit.prevent.once="uploadAudio">
    <NoteUploadAudioForm
      v-if="!!formData"
      v-model="formData"
      :errors="noteFormErrors"
    />
    <input type="submit" value="Save" class="btn btn-primary" />
    <input
      value="Convert to SRT"
      class="btn btn-primary"
      @click="convertToSRT"
    />
  </form>
  <TextArea :field="`convertedSrt`" v-model="convertedSrt" :rows="8" />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { AudioUploadDTO } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import NoteUploadAudioForm from "./NoteUploadAudioForm.vue";
import TextArea from "../../form/TextArea.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  components: {
    NoteUploadAudioForm,
    TextArea,
  },
  props: {
    noteId: { type: Number, required: true },
  },
  emits: ["closeDialog"],
  data() {
    return {
      formData: {} as AudioUploadDTO,
      noteFormErrors: {},
      convertedSrt: "",
    };
  },

  methods: {
    async uploadAudio() {
      try {
        await this.managedApi.restNoteController.uploadAudio(
          this.noteId,
          this.formData,
        );
        this.$emit("closeDialog");
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } catch (error: any) {
        this.noteFormErrors = error;
      }
    },
    async convertToSRT() {
      try {
        const response = await this.managedApi.restAiAudioController.convertSrt(
          this.formData,
        );
        this.convertedSrt = response.srt;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } catch (error: any) {
        this.noteFormErrors = error;
      }
    },
  },
});
</script>

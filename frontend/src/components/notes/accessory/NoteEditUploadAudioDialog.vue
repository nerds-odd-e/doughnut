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
import type { AudioUploadDTO } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { defineComponent } from "vue"
import TextArea from "../../form/TextArea.vue"
import NoteUploadAudioForm from "./NoteUploadAudioForm.vue"

interface ApiError {
  message: string
  code?: number
}

export default defineComponent({
  setup() {
    return { ...useLoadingApi() }
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
    }
  },

  methods: {
    async uploadAudio() {
      try {
        const na = await this.managedApi.restNoteController.uploadAudio(
          this.noteId,
          this.formData
        )
        this.$emit("closeDialog", na)
      } catch (error: unknown) {
        this.noteFormErrors = error as ApiError
      }
    },
    async convertToSRT() {
      try {
        const response = await this.managedApi.restAiAudioController.convertSrt(
          this.formData
        )
        this.convertedSrt = response.srt
      } catch (error: unknown) {
        this.noteFormErrors = error as ApiError
      }
    },
  },
})
</script>

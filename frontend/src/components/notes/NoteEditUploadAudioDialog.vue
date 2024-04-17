<template>
  <form @submit.prevent.once="processForm">
    <NoteUploadAudioForm
      v-if="!!formData"
      v-model="formData"
      :errors="noteFormErrors"
    />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note, NoteAccessories } from "@/generated/backend";
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
    const { ...rest } = this.note.noteAccessories;
    return {
      formData: rest as NoteAccessories,
      noteFormErrors: {},
    };
  },

  methods: {
    processForm() {
      return;
    },
  },
});
</script>

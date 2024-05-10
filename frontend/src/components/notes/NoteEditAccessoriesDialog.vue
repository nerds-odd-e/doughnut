<template>
  <form @submit.prevent.once="processForm">
    <NoteFormBody
      v-if="!!formData"
      v-model="formData"
      :errors="noteFormErrors"
    />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note, NoteAccessoriesDTO } from "@/generated/backend";
import { StorageAccessor } from "../../store/createNoteStorage";
import NoteFormBody from "./NoteFormBody.vue";

export default defineComponent({
  components: {
    NoteFormBody,
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
    const { ...rest } = this.note.noteAccessory || {};
    return {
      formData: rest as NoteAccessoriesDTO,
      noteFormErrors: {},
    };
  },

  methods: {
    processForm() {
      this.storageAccessor
        .storedApi()
        .updateNoteAccessories(this.note.id, this.formData)
        .then(() => this.$emit("closeDialog"))
        .catch((error) => {
          this.noteFormErrors = error;
        });
    },
  },
});
</script>

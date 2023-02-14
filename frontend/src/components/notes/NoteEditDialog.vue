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
import { StorageAccessor } from "../../store/createNoteStorage";
import asPopup from "../commons/Popups/asPopup";
import NoteFormBody from "./NoteFormBody.vue";

export default defineComponent({
  setup() {
    return asPopup();
  },
  components: {
    NoteFormBody,
  },
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  data() {
    const { updatedAt, ...rest } = this.note.noteAccessories;
    return {
      /* eslint-disable  @typescript-eslint/no-explicit-any */
      formData: rest as any,
      noteFormErrors: {},
    };
  },

  methods: {
    processForm() {
      this.storageAccessor
        .api()
        .updateNote(this.note.id, this.formData)
        .then(this.popup.done)
        .catch((error) => {
          this.noteFormErrors = error;
        });
    },
  },
});
</script>

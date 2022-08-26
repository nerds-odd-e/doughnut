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
import { HistoryWriter } from "../../store/history";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import NoteFormBody from "./NoteFormBody.vue";

export default defineComponent({
  setup(props) {
    return useStoredLoadingApi({
      historyWriter: props.historyWriter,
      initalLoading: true,
    });
  },
  name: "NoteEditDialog",
  components: {
    NoteFormBody,
  },
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
      required: true,
    },
  },
  emits: ["done"],
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
      this.storedApi
        .updateNote(this.note.id, this.formData)
        .then(() => {
          this.$emit("done");
        })
        .catch((error) => {
          this.noteFormErrors = error;
        });
    },
  },
});
</script>

<template>
  <form @submit.prevent.once="processForm">
    <NoteFormBody v-if="!!formData" v-model="formData" :errors="formErrors" />
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
      undoHistory: props.historyWriter,
      initalLoading: true,
      hasFormError: true,
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
      formData: rest,
    } as {
      /* eslint-disable  @typescript-eslint/no-explicit-any */
      formData: any;
    };
  },

  methods: {
    processForm() {
      this.storedApi.updateNote(this.note.id, this.formData).then(() => {
        this.$emit("done");
      });
    },
  },
});
</script>

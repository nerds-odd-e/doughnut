<template>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: false });
  },
  props: {
    noteId: { type: Number, required: true },
  },
  mounted() {
      this.api.getNoteComments(this.noteId);
  },
});
</script>

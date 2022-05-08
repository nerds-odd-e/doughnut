<template>
  <button
    class="btn btn-small"
    :title="undoTitle"
    @click="undoDelete()"
    :disabled="!history"
  >
    <SvgUndo />
  </button>
</template>

<script>
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import SvgUndo from "../svgs/SvgUndo.vue";

export default {
  setup() {
    return useStoredLoadingApi();
  },
  name: "NoteUndoButton",
  components: {
    SvgUndo,
  },
  props: {
    noteId: Number,
  },
  computed: {
    history() {
      return this.piniaStore.peekUndo();
    },
    undoTitle() {
      if (this.history) {
        return `undo ${this.history.type}`;
      }
      return "undo";
    },
  },
  methods: {
    undoDelete() {
      this.storedApi.undo();
    },
  },
};
</script>

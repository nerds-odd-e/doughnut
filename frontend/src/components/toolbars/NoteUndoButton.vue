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

<script lang="ts">
import { defineComponent } from "vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import SvgUndo from "../svgs/SvgUndo.vue";

export default defineComponent({
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
  emits: ["noteRealmUpdated"],
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
    async undoDelete() {
      const noteRealm = await this.storedApi.undo();
      this.$emit("noteRealmUpdated", noteRealm);
    },
  },
});
</script>

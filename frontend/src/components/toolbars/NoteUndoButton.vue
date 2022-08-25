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
import { defineComponent, PropType } from "vue";
import { HistoryState } from "../../store/history";
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
    historyState: { type: Object as PropType<HistoryState>, required: true },
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

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
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import SvgUndo from "../svgs/SvgUndo.vue";
import { HistoryState, HistoryWriter } from "../../store/history";

export default defineComponent({
  setup(props) {
    return useStoredLoadingApi({ historyWriter: props.historyWriter });
  },
  name: "NoteUndoButton",
  components: {
    SvgUndo,
  },
  props: {
    noteId: Number,
    histories: {
      type: Object as PropType<HistoryState>,
      required: true,
    },
    historyWriter: {
      type: Object as PropType<HistoryWriter>,
      required: true,
    },
  },
  emits: ["noteRealmUpdated"],
  computed: {
    history() {
      return this.histories.peekUndo();
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

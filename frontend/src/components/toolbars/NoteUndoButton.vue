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
import SvgUndo from "../svgs/SvgUndo.vue";
import { StorageAccessor } from "../../store/history";

export default defineComponent({
  name: "NoteUndoButton",
  components: {
    SvgUndo,
  },
  props: {
    noteId: Number,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["noteRealmUpdated"],
  computed: {
    history() {
      return this.storageAccessor.peekUndo();
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
      const noteRealm = await this.storageAccessor.api().undo();
      this.$emit("noteRealmUpdated", noteRealm);
    },
  },
});
</script>

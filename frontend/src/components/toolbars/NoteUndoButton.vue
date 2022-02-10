<template>
  <button class="btn btn-small" title="undo note" @click="performUndo()">
    <SvgUndo/>
  </button>
</template>

<script>
import SvgUndo from "../svgs/SvgUndo.vue";
import storeUndoCommand from "../../storeUndoCommand";

export default {
  name: "NoteUndoButton",
  components: {
    SvgUndo,
  },
  props: {
    noteId: [String, Number]
  },
  methods: {
    performUndo() {
      storeUndoCommand.popUndoHistory(this.$store, this.noteId);
      const note = this.$store.getters.getNoteById(this.noteId);
    }
  }
};
</script>

<template>
  <button class="btn btn-small" title="undo note" @click="performUndo()">
    <SvgUndo/>
  </button>
  <button class="btn btn-small" title="undo" @click="performUndo()">
    <SvgUndo/>
  </button>
</template>

<script>
import SvgUndo from "../svgs/SvgUndo.vue";
import storeUndoCommand from "../../storeUndoCommand";
import {storedApiUpdateTextContent} from "../../storedApi";

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
      storedApiUpdateTextContent(this.$store, this.noteId, note.textContent)
      .then((res) => {
        this.$emit("done");
      })
    }
  }
};
</script>

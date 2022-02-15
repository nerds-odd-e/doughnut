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
      const noteId = this.$store.getters.peekUndo().noteId;
      storeUndoCommand.popUndoHistory(this.$store);
      const note = this.$store.getters.getNoteById(noteId);
      storedApiUpdateTextContent(this.$store, noteId, note.textContent)
      .then((res) => {
        this.$emit("done");
      })
    }
  }
};
</script>

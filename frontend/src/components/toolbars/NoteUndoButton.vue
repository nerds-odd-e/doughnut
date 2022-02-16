<template>
  <button class="btn btn-small" title="undo note" @click="performUndo()">
    <SvgUndo/>
  </button>
  <button class="btn btn-small" :title="undoTitle" @click="undoDelete()" :disabled="!hasHistory">
    <SvgUndo/>
  </button>
</template>

<script>
import SvgUndo from "../svgs/SvgUndo.vue";
import storeUndoCommand from "../../storeUndoCommand";
import {storedApiUpdateTextContent, storedApiUndoDeleteNote } from "../../storedApi";

export default {
  name: "NoteUndoButton",
  components: {
    SvgUndo,
  },
  props: {
    noteId: [String, Number]
  },
  computed: {
    hasHistory() {
      return (this.$store.getters.peekUndo1() != null)
    },
    undoTitle() {
      if(this.hasHistory) {
        return 'undo delete note'
      }
      return 'undo'
    }
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
    },
    undoDelete() {
      storedApiUndoDeleteNote(this.$store)
    }
  }
};
</script>

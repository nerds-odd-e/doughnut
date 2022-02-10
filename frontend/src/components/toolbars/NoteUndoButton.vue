<template>
  <button class="btn btn-small" title="undo note" @click="performUndo()">
    <SvgUndo />
  </button>
</template>

<script>
import SvgUndo from "../svgs/SvgUndo.vue";

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
      this.$store.commit("popUndoHistory", this.noteId);
      const note = this.$store.getters.getNoteById(this.noteId);
      console.log(JSON.stringify(note, null, 1));
    }
  }
};
</script>

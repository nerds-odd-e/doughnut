<template>
  <button class="btn btn-small" id="undo" @click="undoDelete()" :disabled="!history">
    <SvgUndo/>
  </button>
</template>

<script>
import SvgUndo from "../svgs/SvgUndo.vue";
import { storedApi } from "../../storedApi";

export default {
  name: "NoteUndoButton",
  components: {
    SvgUndo,
  },
  props: {
    noteId: [String, Number]
  },
  computed: {
    history() {
      return this.$store.getters.peekUndo()
    }
  },
  methods: {
    undoDelete() {
      storedApi(this.$store).undo()
    }
  }
};
</script>

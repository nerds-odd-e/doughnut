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
  props: {},
  methods: {
    performUndo() {
        alert('Undo');
    }
  }
};
</script>

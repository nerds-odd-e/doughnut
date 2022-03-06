<template>
  <button class="btn btn-small" :title="undoTitle" @click="undoDelete()" :disabled="!history">
    <SvgUndo/>
  </button>
</template>

<script>
import SvgUndo from "../svgs/SvgUndo.vue";
import storedComponent from '../../store/storedComponent';

export default storedComponent({
  name: "NoteUndoButton",
  components: {
    SvgUndo,
  },
  props: {
    noteId: [String, Number]
  },
  computed: {
    history() {
      return this.piniaStore.peekUndo()
    },
    undoTitle() {
      if(this.history) {
        return `undo ${this.history.type}`
      }
      return 'undo'
    }
  },
  methods: {
    undoDelete() {
      this.storedApi().undo()
    }
  }
});
</script>

<template>
  <button class="btn btn-small" title="undo note" @click="showDialog()">
    <SvgGoBackNote />
  </button>
</template>

<script>
import SvgGoBackNote from "../svgs/SvgGoBack.vue";
import {restPost} from "../../restful/restful";

export default {
  name: "NoteUndoButton",
  components: {
    SvgGoBackNote,
  },
  props: { noteId: Number },
  methods: {
    async showDialog() {
      if (await this.$popups.confirm(`Are you sure you want to go back 1 version for this note?`)) {
        restPost(`/api/notes/${this.noteId}/undo`, {}, (r) => {})
      }
    },
  },
};
</script>

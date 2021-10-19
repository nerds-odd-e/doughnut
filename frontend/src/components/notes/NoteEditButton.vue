<template>
  <button class="btn btn-small" title="edit note" @click="showDialog()">
    <SvgEdit />
  </button>
</template>

<script>
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditDialog from "./NoteEditDialog.vue";

export default {
  name: "NoteNewButton",
  components: {
    SvgEdit,
  },
  props: { noteId: String },
  methods: {
    async showDialog() {
      await this.$popups.dialog(NoteEditDialog, {
        noteId: this.noteId,
      })
    },
  },
};
</script>

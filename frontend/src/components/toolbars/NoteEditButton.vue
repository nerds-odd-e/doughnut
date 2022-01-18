<template>
  <button class="btn btn-small" title="edit note" @click="showDialog()">
    <SvgEdit />
  </button>
</template>

<script>
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditDialog from "../notes/NoteEditDialog.vue";

export default {
  name: "NoteNewButton",
  components: {
    SvgEdit,
  },
  props: { noteId: [String, Number] },
  methods: {
    showDialog() {
      this.$popups.dialog(NoteEditDialog, { noteId: this.noteId })
    },
  },
};
</script>

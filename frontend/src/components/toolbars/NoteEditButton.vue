<template>
  <button class="btn btn-small" title="edit note" @click="showDialog()">
    <SvgEdit />
  </button>
</template>

<script>
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditDialog from "../dialogs/NoteEditDialog.vue";
import { editNote } from "../dialogs";

export default {
  name: "NoteNewButton",
  components: {
    SvgEdit,
  },
  props: { noteId: [String, Number] },
  methods: {
    async showDialog() {
      await editNote(this.$popups, this.noteId);
    },
  },
};
</script>

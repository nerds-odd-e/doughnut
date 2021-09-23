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
  props: { noteId: Number },
  emits: ["updated"],
  methods: {
    async showDialog() {
      if (
        await this.$popups.dialog(NoteEditDialog, {
          noteId: this.noteId,
        })
      ) {
        this.$emit("updated");
      }
    },
  },
};
</script>

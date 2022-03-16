<template>
  <button class="btn btn-small" title="edit note" @click="showDialog()">
    <SvgEdit />
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import NoteEditDialog from "../notes/NoteEditDialog.vue";
import usePopups from "../commons/usePopup";


export default defineComponent({
  name: "NoteNewButton",
  setup() {
    return usePopups();
  },
  components: {
    SvgEdit,
  },
  props: { noteId: Number },
  methods: {
    showDialog() {
      this.popups.dialog(NoteEditDialog, { noteId: this.noteId })
    },
  },
});
</script>

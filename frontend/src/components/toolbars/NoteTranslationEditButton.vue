<template>
  <button
    class="dropdown-item"
    title="edit note translation"
    @click="showDialog()"
  >
    <SvgTranslationEdit />
    <slot />
  </button>
</template>
<script>
import NoteTranslationEditDialog from "../notes/NoteTranslationEditDialog.vue";
import SvgTranslationEdit from "../svgs/SvgTranslationEdit.vue";

export default {
  components: { SvgTranslationEdit },
  props: { noteId: [String, Number] },
  methods: {
    async showDialog() {
      await this.$popups.dialog(NoteTranslationEditDialog, {
        noteId: this.noteId,
      });
    },
  },
};
</script>

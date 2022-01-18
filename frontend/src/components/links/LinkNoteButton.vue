<template>
  <button class="btn btn-small" title="link note" @click="showLinkNoteDialog">
    <SvgLinkNote />
  </button>
</template>

<script>
import LinkNoteDialog from "./LinkNoteDialog.vue";
import SvgLinkNote from "../svgs/SvgLinkNote.vue";

export default {
  name: "LinkNoteButton",
  props: { note: Object },
  components: { SvgLinkNote },
  methods: {
    showLinkNoteDialog() {
      this.$popups.dialog(LinkNoteDialog, { note: this.note })
    },
  },
};
</script>

<template>
  <button class="btn btn-small" title="link note" @click="showLinkNoteDialog">
    <SvgLinkNote />
  </button>
</template>

<script>
import LinkNote from "./LinkNote.vue";
import SvgLinkNote from "../svgs/SvgLinkNote.vue";

export default {
  name: "LinkNoteButton",
  props: { note: Object },
  components: { SvgLinkNote },
  methods: {
    showLinkNoteDialog() {
      this.$popups.dialog(LinkNote, { note: this.note })
    },
  },
};
</script>

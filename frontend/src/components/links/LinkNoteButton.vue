<template>
  <button class="btn btn-small" title="link note" @click="showLinkNoteDialog">
    <SvgSearch />
  </button>
</template>

<script>
import LinkNoteDialog from "./LinkNoteDialog.vue";
import SvgSearch from "../svgs/SvgSearch.vue";

export default {
  name: "LinkNoteButton",
  props: { note: Object },
  components: { SvgSearch },
  methods: {
    showLinkNoteDialog() {
      this.$popups.dialog(LinkNoteDialog, { note: this.note })
    },
  },
};
</script>

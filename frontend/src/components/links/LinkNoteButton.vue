<template>
  <button class="btn btn-small" title="link note" @click="showLinkNoteDialog">
    <SvgSearch />
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import LinkNoteDialog from "./LinkNoteDialog.vue";
import SvgSearch from "../svgs/SvgSearch.vue";
import usePopups from "../commons/usePopup";

export default defineComponent({
  setup() {
    return usePopups();
  },
  name: "LinkNoteButton",
  props: { note: Object },
  components: { SvgSearch },
  methods: {
    showLinkNoteDialog() {
      this.popups.dialog(LinkNoteDialog, { note: this.note })
    },
  },
});
</script>

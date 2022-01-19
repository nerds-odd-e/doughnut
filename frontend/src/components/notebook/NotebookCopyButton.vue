<template>
      <button
          class="btn btn-sm"
          role="button"
          @click="showDialog"
          title="Copy notebook"
      >
        <SvgFailed/>
      </button>
</template>

<script>
import SvgFailed from "../svgs/SvgFailed.vue";
import NotebookCopyDialog from "./NotebookCopyDialog.vue";

export default {
  props: {notebook: Object},
  components: {SvgFailed},
  methods: {
    showDialog() {
      this.$popups.dialog(NotebookCopyDialog, this.$props)
    },
  },
};
</script>

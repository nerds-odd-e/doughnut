<template>
      <button
        class="btn btn-sm"
        role="button"
        @click="showDialog"
        title="Edit notebook settings"
      >
        <SvgEditNotebook />
      </button>
</template>

<script>
import SvgEditNotebook from "../svgs/SvgEditNotebook.vue";
import NotebookEditDialog from "./NotebookEditDialog.vue";

export default {
  props: { notebook: Object },
  components: { SvgEditNotebook },

  methods: {
    showDialog() {
      this.$popups.dialog(NotebookEditDialog, this.$props)
    },
  },
};
</script>

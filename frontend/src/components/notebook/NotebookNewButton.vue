<template>
  <button
    class="btn btn-sm"
    role="button"
    @click="showDialog"
    title="Edit notebook settings"
  >
    <slot />
  </button>
</template>

<script>
import NotebookNewDialog from "./NotebookNewDialog.vue";

export default {
  props: { circle: Object },
  methods: {
    showDialog() {
      this.$popups.dialog(NotebookNewDialog, { circle: this.circle })
    }
  },
};
</script>

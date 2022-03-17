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

<script lang="ts">
import { defineComponent } from "vue";
import usePopups from "../commons/Popups/usePopup";
import NotebookNewDialog from "./NotebookNewDialog.vue";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { circle: Object },
  methods: {
    showDialog() {
      this.popups.dialog(NotebookNewDialog, { circle: this.circle })
    }
  },
});
</script>

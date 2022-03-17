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

<script lang="ts">
import { defineComponent } from "vue";
import usePopups from "../commons/Popups/usePopup";
import SvgEditNotebook from "../svgs/SvgEditNotebook.vue";
import NotebookEditDialog from "./NotebookEditDialog.vue";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { notebook: Object },
  components: { SvgEditNotebook },

  methods: {
    showDialog() {
      this.popups.dialog(NotebookEditDialog, this.$props)
    },
  },
});
</script>

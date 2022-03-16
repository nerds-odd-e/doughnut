<template>
      <button
        class="btn btn-sm"
        role="button"
        @click="showDialog"
        title="Create a new circle"
      >
        Create a new circle
      </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import usePopups from "../commons/usePopup";
import CircleNewDialog from "./CircleNewDialog.vue";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { notebook: Object, user: Object },
  methods: {
    showDialog() {
      this.popups.dialog(CircleNewDialog, this.$props)
    },
  },
});
</script>

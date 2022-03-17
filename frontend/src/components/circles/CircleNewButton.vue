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

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: { notebook: Object, user: Object },
  methods: {
    showDialog() {
      this.popups.dialog1(this.$slots.default)
    },
  },
});
</script>

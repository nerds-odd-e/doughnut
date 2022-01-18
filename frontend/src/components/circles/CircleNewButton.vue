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

<script>
import CircleNewDialog from "./CircleNewDialog.vue";

export default {
  props: { notebook: Object, user: Object },
  methods: {
    showDialog() {
      this.$popups.dialog(CircleNewDialog, this.$props)
    },
  },
};
</script>

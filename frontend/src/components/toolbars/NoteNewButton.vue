<template>
  <button class="btn btn-small" :title="buttonTitle" @click="showDialog">
    <slot />
  </button>
</template>

<script>
import NoteNewDialog from "../notes/NoteNewDialog.vue";

export default {
  props: {
    parentId: [String, Number],
    buttonTitle: String,
  },
  methods: {
    showDialog() {
      this.$popups.dialog(NoteNewDialog, {parentId: this.parentId})
    },
  },
};
</script>

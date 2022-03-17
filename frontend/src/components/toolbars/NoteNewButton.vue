<template>
  <button class="btn btn-small" :title="buttonTitle" @click="showDialog">
    <slot />
  </button>
</template>

<script lang="ts">

import { defineComponent } from "vue";
import usePopups from "../commons/Popups/usePopup";
import NoteNewDialog from "../notes/NoteNewDialog.vue";

export default defineComponent({
  setup() {
    return usePopups();
  },
  props: {
    parentId: Number,
    buttonTitle: String,
  },
  methods: {
    showDialog() {
      this.popups.dialog(NoteNewDialog, {parentId: this.parentId})
    },
  },
});
</script>

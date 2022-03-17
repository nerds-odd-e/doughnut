<template>
  <PopupButton :title="buttonTitle">
    <template v-slot:face>
      <slot />
    </template>
    <template #default="{doneHandler}">
      <NoteNewDialog v-bind="{parentId}" @done="doneHandler($event)"/>
    </template>
  </PopupButton>
</template>

<script lang="ts">

import { defineComponent } from "vue";
import NoteNewDialog from "../notes/NoteNewDialog.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";

export default defineComponent({
  props: {
      parentId: Number,
      buttonTitle: String,
  },
  components: { PopupButton, NoteNewDialog }
});
</script>

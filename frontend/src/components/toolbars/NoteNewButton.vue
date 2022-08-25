<template>
  <PopupButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #dialog_body="{ doneHandler }">
      <NoteNewDialog
        v-bind="{ parentId }"
        @done="
          doneHandler($event);
          $emit('noteRealmUpdated', $event);
        "
      />
    </template>
  </PopupButton>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteNewDialog from "../notes/NoteNewDialog.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";

export default defineComponent({
  props: {
    parentId: { type: Number, required: true },
    buttonTitle: { type: String, required: true },
  },
  emits: ["noteRealmUpdated"],
  components: { PopupButton, NoteNewDialog },
});
</script>

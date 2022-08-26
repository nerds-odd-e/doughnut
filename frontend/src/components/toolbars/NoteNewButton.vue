<template>
  <PopupButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #dialog_body="{ doneHandler }">
      <NoteNewDialog
        v-bind="{ parentId, historyWriter }"
        @done="
          doneHandler($event);
          $emit('noteRealmUpdated', $event);
        "
      />
    </template>
  </PopupButton>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteNewDialog from "../notes/NoteNewDialog.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  props: {
    parentId: { type: Number, required: true },
    buttonTitle: { type: String, required: true },
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
    },
  },
  emits: ["noteRealmUpdated"],
  components: { PopupButton, NoteNewDialog },
});
</script>

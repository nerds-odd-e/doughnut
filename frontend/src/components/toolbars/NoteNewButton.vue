<template>
  <PopupButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #dialog_body="{ doneHandler }">
      <NoteNewDialog
        v-bind="{ parentId, storageAccessor }"
        @done="doneHandler($event)"
      />
    </template>
  </PopupButton>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteNewDialog from "../notes/NoteNewDialog.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    parentId: { type: Number, required: true },
    buttonTitle: { type: String, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { PopupButton, NoteNewDialog },
});
</script>

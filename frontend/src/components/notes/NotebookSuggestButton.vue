<template>
  <PopupButton class="btn-primary" title="Suggest">
    <template #button_face>
      <slot />
    </template>
    <template #dialog_body="{ doneHandler }">
      <NotebookSuggestDialog
        v-bind="{ note: selectedNote, storageAccessor }"
        @done="doneHandler($event)"
      />
    </template>
  </PopupButton>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NotebookSuggestDialog from "./NotebookSuggestDialog.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { PopupButton, NotebookSuggestDialog },
});
</script>

<template>
  <PopupButton class="btn-primary" title="Engaging Story">
    <template #button_face>
      <slot />
    </template>
    <template #dialog_body="{ doneHandler }">
      <NoteStoryDialog
        v-bind="{ selectedNoteId, storageAccessor }"
        @done="doneHandler($event)"
      />
    </template>
  </PopupButton>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteStoryDialog from "@/components/notes/NoteEngagingStoryDialog.vue";
import PopupButton from "../commons/Popups/PopupButton.vue";
import type { StorageAccessor } from "@/store/createNoteStorage";

export default defineComponent({
  props: {
    selectedNoteId: { type: Number, required: false },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: false,
    },
  },
  components: { NoteStoryDialog, PopupButton },
});
</script>

<style lang="sass" scoped>
.btn-primary
  align-items: center
  margin-left: .5em
</style>

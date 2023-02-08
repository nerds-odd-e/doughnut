<template>
  <PopupButton class="btn-primary" title="Engaging Story">
    <template #button_face>
      <slot />
    </template>
    <template #dialog_body="{}">
      <NoteStoryDialog v-bind="{ selectedNote, storageAccessor }" />
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
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
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

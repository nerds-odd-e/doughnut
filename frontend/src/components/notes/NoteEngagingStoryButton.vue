<template>
  <PopButton class="btn-primary" title="Engaging Story">
    <template #button_face>
      <slot />
    </template>
    <NoteStoryDialog v-bind="{ selectedNoteId, storageAccessor }" />
  </PopButton>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteStoryDialog from "@/components/notes/NoteEngagingStoryDialog.vue";
import PopButton from "../commons/Popups/PopButton.vue";
import type { StorageAccessor } from "@/store/createNoteStorage";

export default defineComponent({
  props: {
    selectedNoteId: { type: Number, required: false },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: false,
    },
  },
  components: { NoteStoryDialog, PopButton },
});
</script>

<style lang="sass" scoped>
.btn-primary
  align-items: center
  margin-left: .5em
</style>

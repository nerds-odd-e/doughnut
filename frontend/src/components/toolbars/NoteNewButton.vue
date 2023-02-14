<template>
  <PopButton :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <NoteNewDialog v-bind="{ parentId, storageAccessor }" />
  </PopButton>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteNewDialog from "../notes/NoteNewDialog.vue";
import PopButton from "../commons/Popups/PopButton.vue";
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
  components: { PopButton, NoteNewDialog },
});
</script>

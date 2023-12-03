<template>
  <PopButton1 :title="buttonTitle">
    <template #button_face>
      <slot />
    </template>
    <template #default="{ closer }">
      <NoteNewDialog
        v-bind="{ parentId, storageAccessor }"
        @close-dialog="closer"
      />
    </template>
  </PopButton1>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteNewDialog from "../notes/NoteNewDialog.vue";
import PopButton1 from "../commons/Popups/PopButton1.vue";
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
  components: { PopButton1, NoteNewDialog },
});
</script>

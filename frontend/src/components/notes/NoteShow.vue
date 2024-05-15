<template>
  <BreadcrumbAsync v-bind="{ noteId }" />
  <NoteShowInner
    v-bind="{ noteId, expandChildren, expandInfo, readonly, storageAccessor }"
    @level-changed="$emit('levelChanged', $event)"
    @self-evaluated="$emit('selfEvaluated', $event)"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteShowInner from "./NoteShowInner.vue";
import BreadcrumbAsync from "../toolbars/BreadcrumbAsync.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    expandChildren: { type: Boolean, required: true },
    expandInfo: { type: Boolean, default: false },
    readonly: { type: Boolean, default: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["levelChanged", "selfEvaluated"],
  components: {
    NoteShowInner,
    BreadcrumbAsync,
  },
});
</script>

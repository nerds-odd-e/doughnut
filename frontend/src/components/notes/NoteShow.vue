<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <Breadcrumb v-bind="{ notePosition: noteRealm.notePosition }" />
      <NoteShowInner
        v-bind="{
          noteRealm,
          expandChildren,
          expandInfo,
          readonly,
          storageAccessor,
        }"
        @level-changed="$emit('levelChanged', $event)"
      />
    </template>
  </NoteRealmLoader>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
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
  emits: ["levelChanged"],
  components: {
    Breadcrumb,
  },
});
</script>

<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <Breadcrumb v-bind="{ notePosition: noteRealm.notePosition }" />
      <NoteShowInner
        v-bind="{
          noteRealm,
          expandChildren,
          readonly,
          storageAccessor,
        }"
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
    readonly: { type: Boolean, default: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    Breadcrumb,
  },
});
</script>

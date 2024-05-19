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

<script setup lang="ts">
import { PropType, defineProps } from "vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});
</script>

<template>
  <template v-if="noteRealm.note.parentId">
    <NoteRealmLoader
      v-bind="{ noteId: noteRealm.note.parentId, storageAccessor }"
    >
      <template #default="{}"> </template>
    </NoteRealmLoader>
  </template>
  <ChildrenNotes
    v-bind="{ expandChildren: true, readonly: false, storageAccessor }"
    :notes="noteRealm.children ?? []"
  />
</template>

<script setup lang="ts">
import { PropType } from "vue";
import { NoteRealm } from "@/generated/backend";
import ChildrenNotes from "./ChildrenNotes.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

defineProps({
  noteRealm: { type: Object as PropType<NoteRealm>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});
</script>

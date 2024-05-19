<template>
  <template v-if="noteRealm.note.parentId">
    <NoteRealmLoader
      v-bind="{ noteId: noteRealm.note.parentId, storageAccessor }"
    >
      <template #default="{ noteRealm: nr }">
        <ChildrenNotes
          v-bind="{ expandChildren: true, readonly: false, storageAccessor }"
          :notes="nr.children ?? []"
        />
      </template>
    </NoteRealmLoader>
  </template>
  <ChildrenNotes
    v-else
    v-bind="{ expandChildren: true, readonly: false, storageAccessor }"
    :notes="noteRealm.children ?? []"
  />
</template>

<script setup lang="ts">
import { PropType } from "vue";
import { NoteRealm } from "@/generated/backend";
import ChildrenNotes from "./ChildrenNotes.vue";
import NoteRealmLoader from "./NoteRealmLoader.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

defineProps({
  noteRealm: { type: Object as PropType<NoteRealm>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});
</script>

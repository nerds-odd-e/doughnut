<template>
  <template v-if="noteId !== activeNoteRealm.id">
    <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
      <template #default="{ noteRealm: nr }">
        <SidebarLevel
          v-bind="{
            activeNoteRealm,
            storageAccessor,
          }"
          :notes="nr.children ?? []"
        />
      </template>
    </NoteRealmLoader>
  </template>
  <SidebarLevel
    v-else
    v-bind="{ activeNoteRealm, storageAccessor }"
    :notes="activeNoteRealm.children ?? []"
  />
</template>

<script setup lang="ts">
import { PropType } from "vue";
import { NoteRealm } from "@/generated/backend";
import SidebarLevel from "./SidebarLevel.vue";
import NoteRealmLoader from "./NoteRealmLoader.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

defineProps({
  noteId: { type: Number, required: true },
  activeNoteRealm: { type: Object as PropType<NoteRealm>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});
</script>

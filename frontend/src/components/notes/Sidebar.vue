<template>
  <SidebarInner
    v-bind="{ noteId: headNoteId, activeNoteRealm: noteRealm, storageAccessor }"
  />
</template>

<script setup lang="ts">
import { PropType, computed } from "vue";
import { NoteRealm } from "@/generated/backend";
import { first } from "lodash";
import SidebarInner from "./SidebarInner.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteRealm: { type: Object as PropType<NoteRealm>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const headNoteId = computed(() => {
  const { ancestors } = props.noteRealm.notePosition;
  return first(ancestors)?.id ?? props.noteRealm.note.id;
});
</script>

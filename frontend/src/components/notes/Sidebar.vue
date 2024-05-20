<template>
  <SidebarInner
    v-if="noteRealm && headNoteId"
    v-bind="{ noteId: headNoteId, activeNoteRealm: noteRealm, storageAccessor }"
    :key="headNoteId"
  />
</template>

<script setup lang="ts">
import { PropType, computed, toRefs } from "vue";
import { NoteRealm } from "@/generated/backend";
import { first } from "lodash";
import SidebarInner from "./SidebarInner.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteRealm: { type: Object as PropType<NoteRealm> },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const reactiveProps = toRefs(props);

const headNoteId = computed(() => {
  if (!reactiveProps.noteRealm?.value) return undefined;
  const { ancestors } = reactiveProps.noteRealm.value.notePosition;
  return first(ancestors)?.id ?? reactiveProps.noteRealm.value.note.id;
});
</script>

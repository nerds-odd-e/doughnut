<template>
  <SidebarInner
    :class="{ 'is-disabled': !noteRealm }"
    v-if="lastDefinedNoteRealm && headNoteId"
    v-bind="{
      noteId: headNoteId,
      activeNoteRealm: lastDefinedNoteRealm,
      storageAccessor,
    }"
    :key="headNoteId"
  />
</template>

<script setup lang="ts">
import { PropType, Ref, computed, ref, toRefs, watch } from "vue";
import { NoteRealm } from "@/generated/backend";
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

const lastDefinedNoteRealm: Ref<NoteRealm | undefined> = ref(undefined);

watch(
  () => reactiveProps.noteRealm?.value,
  (newNoteRealm) => {
    if (newNoteRealm !== undefined) {
      lastDefinedNoteRealm.value = newNoteRealm;
    }
  },
  { immediate: true },
);

const headNoteId = computed(() => {
  const noteRealm = lastDefinedNoteRealm.value;
  if (!noteRealm) return undefined;
  let cursor = noteRealm.note.noteTopic;
  while (cursor.parentNoteTopic) {
    cursor = cursor.parentNoteTopic;
  }
  return cursor.id;
});
</script>

<style scoped>
.is-disabled {
  opacity: 0.5;
  pointer-events: none;
}
</style>

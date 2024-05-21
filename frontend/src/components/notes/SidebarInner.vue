<template>
  <ul v-if="(noteRealm?.children?.length ?? 0) > 0" class="list-group">
    <li
      v-for="note in noteRealm?.children"
      :key="note.id"
      class="list-group-item list-group-item-action pb-0 pe-0 border-0"
    >
      <NoteTopicWithLink class="w-100 card-title" v-bind="{ note }" />
      <SidebarInner
        v-if="expandedIds.some((id) => id === note.id)"
        v-bind="{
          noteId: note.id,
          activeNoteRealm,
          storageAccessor,
        }"
        :key="note.id"
      />
    </li>
  </ul>
</template>

<script setup lang="ts">
import { PropType, ref, watch } from "vue";
import { NoteRealm } from "@/generated/backend";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteId: { type: Number, required: true },
  activeNoteRealm: { type: Object as PropType<NoteRealm>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const noteRealm = props.storageAccessor
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.noteId);

const expandedIds = ref([props.activeNoteRealm.note.id]);

watch(
  () => props.activeNoteRealm.notePosition.ancestors,
  (newAncestors) => {
    const uniqueIds = new Set([
      ...expandedIds.value,
      ...(newAncestors?.map((note) => note.id) ?? []),
      props.activeNoteRealm.note.id,
    ]);
    expandedIds.value = Array.from(uniqueIds);
  },
  { immediate: true },
);
</script>

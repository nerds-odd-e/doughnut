<template>
  <div v-if="(noteRealm?.children?.length ?? 0) > 0" class="row">
    <div class="col-auto bg-light p-0" style="width: 40px"></div>
    <div class="col">
      <div class="row">
        <div v-for="note in noteRealm?.children" :key="note.id">
          <NoteTopicWithLink class="w-100 card-title" v-bind="{ note }" />
          <SidebarInner
            v-if="expandedIds.some((id) => id === note.id)"
            v-bind="{
              noteId: note.id,
              activeNoteRealm,
              storageAccessor,
            }"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue";
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

const expandedIds = ref([
  ...(props.activeNoteRealm.notePosition.ancestors?.map((note) => note.id) ??
    []),
  props.activeNoteRealm.note.id,
]);
</script>

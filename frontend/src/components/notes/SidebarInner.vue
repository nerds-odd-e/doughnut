<template>
  <NoteRealmLoader
    v-bind="{ noteId, justLoaded: activeNoteRealm, storageAccessor }"
  >
    <template #default="{ noteRealm: nr }">
      <div v-if="(nr.children?.length ?? 0) > 0" class="row">
        <div class="col-auto bg-light p-0" style="width: 40px"></div>
        <div class="col">
          <div class="row">
            <div v-for="note in nr.children" :key="note.id">
              <NoteTopicWithLink class="w-100 card-title" v-bind="{ note }" />
              <SidebarInner
                v-if="
                  activeNoteRealm.id === note.id ||
                  inActiveNoteAncestors(note.id)
                "
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
  </NoteRealmLoader>
</template>

<script setup lang="ts">
import { PropType } from "vue";
import { NoteRealm } from "@/generated/backend";
import NoteRealmLoader from "./NoteRealmLoader.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteId: { type: Number, required: true },
  activeNoteRealm: { type: Object as PropType<NoteRealm>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const inActiveNoteAncestors = (id: number) => {
  return props.activeNoteRealm.notePosition.ancestors?.some(
    (note) => note.id === id,
  );
};
</script>

<template>
  <div v-if="notes.length > 0" class="row">
    <div class="col-auto bg-light p-0" style="width: 40px"></div>
    <div class="col">
      <div class="row">
        <div v-for="note in notes" :key="note.id">
          <NoteTopicWithLink class="w-100 card-title" v-bind="{ note }" />
          <SidebarLevel
            v-if="activeNoteRealm.id === note.id"
            v-bind="{
              notes: activeNoteRealm.children ?? [],
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
import { PropType } from "vue";
import { Note, NoteRealm } from "@/generated/backend";
import { StorageAccessor } from "@/store/createNoteStorage";
import NoteTopicWithLink from "./NoteTopicWithLink.vue";

defineProps({
  notes: { type: Array as PropType<Note[]>, required: true },
  activeNoteRealm: { type: Object as PropType<NoteRealm>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});
</script>

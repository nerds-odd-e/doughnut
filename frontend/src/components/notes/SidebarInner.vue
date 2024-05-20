<template>
  <div v-if="(noteRealm?.children?.length ?? 0) > 0" class="row">
    <div class="col-auto bg-light p-0" style="width: 40px"></div>
    <div class="col">
      <div class="row">
        <div v-for="note in noteRealm?.children" :key="note.id">
          <NoteTopicWithLink class="w-100 card-title" v-bind="{ note }" />
          <SidebarInner
            v-if="
              activeNoteRealm.id === note.id || inActiveNoteAncestors(note.id)
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

const inActiveNoteAncestors = (id: number) => {
  return props.activeNoteRealm.notePosition.ancestors?.some(
    (note) => note.id === id,
  );
};

const noteRealm =
  props.activeNoteRealm.id === props.noteId
    ? ref(props.activeNoteRealm)
    : props.storageAccessor.refOfNoteRealm(props.noteId);

if (props.activeNoteRealm.id !== props.noteId) {
  props.storageAccessor.storedApi().getNoteRealmAndReloadPosition(props.noteId);
}
</script>

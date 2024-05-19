<template>
  <LoadingPage v-bind="{ contentExists: !!noteRealm }">
    <slot v-if="noteRealm" :note-realm="noteRealm" />
  </LoadingPage>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import { NoteRealm } from "@/generated/backend";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteId: { type: Number, required: true },
  justLoaded: { type: Object as PropType<NoteRealm>, required: false },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const noteRealm =
  props.justLoaded?.id === props.noteId
    ? ref(props.justLoaded)
    : props.storageAccessor.refOfNoteRealm(props.noteId);

if (props.justLoaded?.id !== props.noteId) {
  props.storageAccessor.storedApi().getNoteRealmAndReloadPosition(props.noteId);
}
</script>

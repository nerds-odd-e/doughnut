<template>
  <LoadingPage v-bind="{ contentExists: !!noteRealm }">
    <slot v-if="noteRealm" :note-realm="noteRealm" />
  </LoadingPage>
</template>

<script setup lang="ts">
import { PropType } from "vue";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const noteRealm = props.storageAccessor.refOfNoteRealm(props.noteId);
props.storageAccessor.storedApi().getNoteRealmAndReloadPosition(props.noteId);
</script>

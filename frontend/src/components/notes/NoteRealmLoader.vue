<template>
  <slot :note-realm="noteRealm" />
</template>

<script setup lang="ts">
import { PropType, computed, toRefs } from "vue";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const reactiveProps = toRefs(props);

const noteRealmRef = computed(() =>
  reactiveProps.storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndReloadPosition(reactiveProps.noteId.value),
);

const noteRealm = computed(() => noteRealmRef.value?.value);
</script>

<template>
  <NoteRealmLoader v-bind="{ noteId: currentNoteId.id, storageAccessor }">
    <template #default="{ noteRealm }">
      <slot :note-realm="noteRealm" />
    </template>
  </NoteRealmLoader>
</template>

<script setup lang="ts">
import { PropType, computed, toRefs } from "vue";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const emit = defineEmits(["toggle-sidebar"]);

const reactiveProps = toRefs(props);

const currentNoteIdRef = computed(() =>
  reactiveProps.storageAccessor.value.currentNoteIdRef(),
);
const currentNoteId = computed(() => currentNoteIdRef.value?.value);
</script>

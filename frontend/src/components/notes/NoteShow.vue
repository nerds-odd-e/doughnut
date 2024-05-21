<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <div class="container">
        <ContentLoader v-if="!noteRealm" />
        <div v-else>
          <NoteShowInner
            v-bind="{
              noteRealm,
              expandChildren,
              readonly,
              storageAccessor,
            }"
            :key="noteId"
          />
        </div>
      </div>
    </template>
  </NoteRealmLoader>
</template>

<script setup lang="ts">
import { PropType, toRefs, watch } from "vue";
import ContentLoader from "@/components/commons/ContentLoader.vue";
import NoteRealmLoader from "./NoteRealmLoader.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const reactiveProps = toRefs(props);

watch(
  () => reactiveProps.noteId.value,
  (newNoteId) => {
    reactiveProps.storageAccessor.value.currentNoteIdRef().value = {
      id: newNoteId,
    };
  },
  { immediate: true },
);
</script>

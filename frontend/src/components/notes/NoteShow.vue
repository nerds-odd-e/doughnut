<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <Breadcrumb v-bind="{ notePosition: noteRealm.notePosition }" />
      <NoteShowInner
        v-bind="{
          noteRealm,
          expandChildren,
          readonly,
          highlightNoteId,
          storageAccessor,
        }"
        @highlight-note="highlightNoteId = $event"
      />
    </template>
  </NoteRealmLoader>
</template>

<script setup lang="ts">
import { PropType, defineProps, ref } from "vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
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

const highlightNoteId = ref(props.noteId);
</script>

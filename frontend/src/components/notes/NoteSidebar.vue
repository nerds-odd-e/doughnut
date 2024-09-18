<template>
  <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
    <template #default="{ noteRealm }">
      <Sidebar
        v-bind="{
          noteRealm,
          storageAccessor,
        }"
      />
    </template>
  </NoteRealmLoader>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { StorageAccessor } from "../../store/createNoteStorage"

defineProps({
  noteId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>

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
import type { PropType } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import NoteRealmLoader from "./NoteRealmLoader.vue"

defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  readonly: { type: Boolean, default: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>

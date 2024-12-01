<template>
  <div class="note-show-container">
    <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
      <template #default="{ noteRealm }">
        <ContentLoader v-if="!noteRealm" />
        <NoteShowInner
          v-else
          v-bind="{
            noteRealm,
            expandChildren,
            storageAccessor,
            noConversationButton,
          }"
          :key="noteId"
        />
      </template>
    </NoteRealmLoader>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import NoteRealmLoader from "./NoteRealmLoader.vue"
import NoteShowInner from "./NoteShowInner.vue"

defineProps({
  noteId: { type: Number, required: true },
  expandChildren: { type: Boolean, required: true },
  noConversationButton: { type: Boolean, default: false },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>

<style scoped>
.note-show-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}
</style>

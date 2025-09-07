<template>
  <div class="p-4">
    <h3 class="text-lg font-bold mb-4">Import from Obsidian</h3>
    <!-- Add your import logic here -->
    <div class="daisy-alert daisy-alert-info">
      <span>This feature is coming soon!</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"

defineProps<{
  note: Note
  storageAccessor: StorageAccessor
}>()
</script> 
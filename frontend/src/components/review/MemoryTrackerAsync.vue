<template>
  <ContentLoader v-if="!memoryTracker" />
  <main v-else>
    <NoteShow
      v-bind="{
        noteId: memoryTracker.note.id,
        expandChildren: false,
        storageAccessor,
      }"
    />
  </main>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type { MemoryTracker } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import type { StorageAccessor } from "@/store/createNoteStorage"
import NoteShow from "../notes/NoteShow.vue"

// Props definition
const props = defineProps<{
  memoryTrackerId: number
  storageAccessor: StorageAccessor
}>()

// Setup state
const memoryTracker = ref<MemoryTracker>()

// Methods
const fetchData = async () => {
  const { data: tracker, error } =
    await MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: props.memoryTrackerId },
    })
  if (!error) {
    memoryTracker.value = tracker!
  }
}

// Watchers and lifecycle hooks
watch(
  () => props.memoryTrackerId,
  () => {
    fetchData()
  }
)

onMounted(() => {
  fetchData()
})
</script>

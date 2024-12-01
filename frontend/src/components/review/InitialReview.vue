<template>
  <ContainerPage>
    <main>
      <NoteWithBreadcrumb v-bind="{ note, storageAccessor }" />
    </main>
    <NoteInfoBar
      :note-id="note.id"
      :key="note.id"
      @level-changed="$emit('reloadNeeded', $event)"
    />
    <InitialReviewButtons
      :key="buttonKey"
      @do-initial-review="processForm"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import type { Note, MemoryTracker } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import usePopups from "../commons/Popups/usePopups"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import InitialReviewButtons from "./InitialReviewButtons.vue"
import NoteWithBreadcrumb from "./NoteWithBreadcrumb.vue"
import { computed } from "vue"

// Props
const { note } = defineProps<{
  note: Note
  storageAccessor: StorageAccessor
}>()

// Emits
const emit = defineEmits<{
  (e: "reloadNeeded", data: MemoryTracker): void
  (e: "initialReviewDone", data: MemoryTracker): void
}>()

// Composables
const { managedApi } = useLoadingApi()
const { popups } = usePopups()

// Computed
const buttonKey = computed(() => note.id)

// Methods
const processForm = async (skipMemoryTracking: boolean) => {
  if (skipMemoryTracking) {
    if (
      !(await popups.confirm(
        "Confirm to hide this note from reviewing in the future?"
      ))
    ) {
      return
    }
  }

  managedApi.memoryTrackerOnboardingController
    .onboard({
      noteId: note.id,
      skipMemoryTracking,
    })
    .then((data) => {
      if (skipMemoryTracking) {
        emit("reloadNeeded", data)
      } else {
        emit("initialReviewDone", data)
      }
    })
}
</script>

<style>
.initial-review-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

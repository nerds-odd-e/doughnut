<template>
  <ContainerPage>
    <main>
      <NoteShow
        v-bind="{ noteId: note.id, storageAccessor, expandChildren: false }"
      />
    </main>
    <NoteInfoBar
      :note-id="note.id"
      :key="note.id"
      @level-changed="$emit('reloadNeeded')"
    />
    <AssimilationButtons
      :key="buttonKey"
      @assimilate="processForm"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import { assimilate } from "@generated/backend/sdk.gen"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import usePopups from "../commons/Popups/usePopups"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import NoteShow from "../notes/NoteShow.vue"
import { computed } from "vue"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"

// Props
const { note } = defineProps<{
  note: Note
  storageAccessor: StorageAccessor
}>()

// Emits
const emit = defineEmits<{
  (e: "reloadNeeded"): void
  (e: "initialReviewDone"): void
}>()

// Composables
const { popups } = usePopups()
const { totalAssimilatedCount } = useRecallData()

const { incrementAssimilatedCount } = useAssimilationCount()

// Computed
const buttonKey = computed(() => note.id)

// Methods
const processForm = async (skipMemoryTracking: boolean) => {
  if (skipMemoryTracking) {
    const confirmed = await popups.confirm(
      "Confirm to hide this note from reviewing in the future?"
    )
    if (!confirmed) {
      return
    }
  }

  const { data: memoryTrackers, error } = await assimilate({
    body: {
      noteId: note.id,
      skipMemoryTracking,
    },
  })

  if (!error && memoryTrackers) {
    const newTrackerCount = memoryTrackers.filter(
      (t) => !t.removedFromTracking
    ).length
    if (totalAssimilatedCount.value !== undefined) {
      totalAssimilatedCount.value += newTrackerCount
    }
    incrementAssimilatedCount(newTrackerCount)

    if (skipMemoryTracking) {
      emit("reloadNeeded")
    } else {
      emit("initialReviewDone")
    }
  }
}
</script>

<style>
.initial-review-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

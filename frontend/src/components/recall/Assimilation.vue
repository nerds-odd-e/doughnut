<template>
  <main>
    <div class="breadcrumb-wrapper daisy-mb-2">
      <Breadcrumb
        v-bind="{ noteTopology: note.noteTopology, includingSelf: false }"
      />
    </div>
    <NoteShow
      v-bind="{ noteId: note.id, expandChildren: false }"
      @details-saved="onDetailsSaved"
    />
  </main>
  <NoteInfoBar
    :note-id="note.id"
    :current-note-details="currentDetails"
    :key="note.id"
    @level-changed="$emit('reloadNeeded')"
    @note-type-updated="onNoteTypeUpdated"
    @note-info-loaded="onNoteInfoLoaded"
    @remember-spelling-changed="onRememberSpellingChanged"
  />
  <NoteRefinement
    :note="note"
    :current-note-details="currentDetails"
    :refresh-trigger="refinementRefreshTrigger"
    @reload-needed="$emit('reloadNeeded')"
  />
  <AssimilationButtons
    :key="buttonKey"
    :disabled="!noteInfoLoaded"
    @assimilate="processForm"
  />
  <SpellingVerificationPopup
    :show="showSpellingPopup"
    :note-id="note.id"
    @cancel="handleSpellingCancel"
    @verified="handleSpellingVerified"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import { AssimilationController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import NoteRefinement from "./NoteRefinement.vue"
import NoteShow from "../notes/NoteShow.vue"
import Breadcrumb from "../toolbars/Breadcrumb.vue"
import SpellingVerificationPopup from "./SpellingVerificationPopup.vue"
import { computed, ref } from "vue"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"

const { note } = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "reloadNeeded"): void
  (e: "initialReviewDone"): void
}>()

// Composables
const { popups } = usePopups()
const { totalAssimilatedCount } = useRecallData()

const { incrementAssimilatedCount } = useAssimilationCount()

// State
const buttonKey = computed(() => note.id)
const showSpellingPopup = ref(false)
const rememberSpelling = ref(false)
const noteInfoLoaded = ref(false) // Track if noteInfo has been loaded
const currentDetails = ref(note.details)

const onDetailsSaved = (newDetails: string) => {
  currentDetails.value = newDetails
}

const refinementRefreshTrigger = ref(0)

const onNoteInfoLoaded = () => {
  refinementRefreshTrigger.value++
}

const onRememberSpellingChanged = (value: boolean) => {
  rememberSpelling.value = value
  noteInfoLoaded.value = true // Mark as loaded when we receive the value
}

const onNoteTypeUpdated = () => {
  refinementRefreshTrigger.value++
}

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

  // If rememberSpelling is checked and not skipping, show verification popup
  if (!skipMemoryTracking && rememberSpelling.value) {
    showSpellingPopup.value = true
    return
  }

  await doAssimilate(skipMemoryTracking)
}

const doAssimilate = async (skipMemoryTracking: boolean) => {
  const { data: memoryTrackers, error } = await apiCallWithLoading(() =>
    AssimilationController.assimilate({
      body: {
        noteId: note.id,
        skipMemoryTracking,
      },
    })
  )

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

const handleSpellingVerified = () => {
  showSpellingPopup.value = false
  doAssimilate(false)
}

const handleSpellingCancel = () => {
  showSpellingPopup.value = false
}
</script>

<style>
.initial-review-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

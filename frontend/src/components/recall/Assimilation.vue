<template>
  <main>
    <div class="breadcrumb-wrapper daisy-mb-2">
      <Breadcrumb
        v-bind="{ noteTopology: note.noteTopology, includingSelf: false }"
      />
    </div>
    <NoteShow
      v-bind="{ noteId: note.id, expandChildren: false }"
    />
  </main>
  <NoteInfoBar
    :note-id="note.id"
    :key="note.id"
    @level-changed="$emit('reloadNeeded')"
  />
  <div
    v-if="showNoteTypeSelection"
    data-test="note-type-selection-dialog"
    class="daisy-mb-4"
    style="text-align: left;"
  >
    <label class="daisy-label" style="justify-content: flex-start;">
      <span class="daisy-label-text">Select Note Type:</span>
      <select
        v-model="selectedNoteType"
        class="daisy-select daisy-select-bordered"
        style="text-align: left; width: 100%;"
      >
        <option value="">Please select...</option>
        <option value="concept">concept</option>
        <option value="category">category</option>
        <option value="vocab">vocab</option>
        <option value="journal">journal</option>
        <option value="unassigned">unassigned</option>
      </select>
    </label>
  </div>
  <AssimilationButtons
    :key="buttonKey"
    @assimilate="processForm"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import {
  AssimilationController,
  NoteController,
} from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import NoteShow from "../notes/NoteShow.vue"
import Breadcrumb from "../toolbars/Breadcrumb.vue"
import { computed, ref, watch } from "vue"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"

// Props
const { note } = defineProps<{
  note: Note
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

// State
const selectedNoteType = ref(note.noteType || "")

// Always show dialog so user can update note type if they wish
const showNoteTypeSelection = ref(true)

const buttonKey = computed(() => note.id)

// Update selectedNoteType when note prop changes (new note loaded)
watch(
  () => note.noteType,
  (newNoteType) => {
    selectedNoteType.value = newNoteType || ""
  }
)

// Watch for user changes and save to database
watch(selectedNoteType, async (newType, oldType) => {
  // Don't save on initial mount or if value hasn't actually changed
  if (!newType || newType === "" || newType === oldType) {
    return
  }

  // Don't save if it matches the current note.noteType (avoid unnecessary API calls)
  if (newType === note.noteType) {
    return
  }

  const previousValue = note.noteType || ""

  const { error } = await apiCallWithLoading(() =>
    NoteController.updateNoteType({
      path: { note: note.id },
      body: newType as
        | "concept"
        | "category"
        | "vocab"
        | "journal"
        | "unassigned",
    })
  )

  if (error) {
    // Revert to previous value on error
    selectedNoteType.value = previousValue
  }
})

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
</script>

<style>
.initial-review-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}

[data-test="note-type-selection-dialog"] {
  text-align: left;
}

[data-test="note-type-selection-dialog"] .daisy-label {
  justify-content: flex-start !important;
  align-items: flex-start !important;
}

[data-test="note-type-selection-dialog"] .daisy-label-text {
  text-align: left;
}

[data-test="note-type-selection-dialog"] select {
  text-align: left !important;
  direction: ltr;
}

[data-test="note-type-selection-dialog"] select option {
  text-align: left;
  direction: ltr;
}
</style>

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
    @note-type-updated="onNoteTypeUpdated"
    @note-info-loaded="onNoteInfoLoaded"
  />
  <div
    v-if="noteSummaryPoints.length > 0 || shouldShowCategoryMessage"
    data-test="note-details-summary"
    class="daisy-mb-4 daisy-alert daisy-alert-info"
  >
    <div class="daisy-text-sm">
      <div v-if="shouldShowCategoryMessage" class="daisy-text-base-content">
        No summary requested for initiative notes.
      </div>
      <template v-else>
        <div class="daisy-font-semibold daisy-mb-2 daisy-text-base-content">
          Summary:
        </div>
        <ul class="daisy-list-disc daisy-list-inside daisy-space-y-1">
          <li
            v-for="(point, index) in noteSummaryPoints"
            :key="index"
            class="daisy-text-base-content"
          >
            {{ point }}
          </li>
        </ul>
      </template>
    </div>
  </div>
  <AssimilationButtons
    :key="buttonKey"
    @assimilate="processForm"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import {
  AiController,
  AssimilationController,
} from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import NoteShow from "../notes/NoteShow.vue"
import Breadcrumb from "../toolbars/Breadcrumb.vue"
import { computed, ref } from "vue"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import type { NoteType } from "@/models/noteTypeOptions"

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
const currentNoteType = ref<NoteType | undefined>(undefined)

const buttonKey = computed(() => note.id)

// Summary from backend
const noteSummaryPoints = ref<string[]>([])
const isLoadingSummary = ref(false)

const shouldShowCategoryMessage = computed(() => {
  return (
    currentNoteType.value === "initiative" &&
    note.details &&
    note.details.trim().length > 0
  )
})

const generateSummary = async () => {
  if (!note.details || note.details.trim().length === 0) {
    noteSummaryPoints.value = []
    return
  }

  // Skip summary generation for initiative note type
  if (currentNoteType.value === "initiative") {
    noteSummaryPoints.value = []
    return
  }

  isLoadingSummary.value = true
  try {
    const result = await apiCallWithLoading(() =>
      AiController.generateSummary({
        path: { note: note.id },
      })
    )

    if (!result.error && result.data) {
      noteSummaryPoints.value = result.data.points || []
    } else {
      noteSummaryPoints.value = []
    }
  } catch (err) {
    console.error("Failed to generate summary:", err)
    noteSummaryPoints.value = []
  } finally {
    isLoadingSummary.value = false
  }
}

const onNoteInfoLoaded = (noteType: NoteType | undefined) => {
  currentNoteType.value = noteType
  generateSummary()
}

const onNoteTypeUpdated = (newType: NoteType | undefined) => {
  currentNoteType.value = newType
  // Regenerate summary after note type is updated (to handle initiative exclusion)
  generateSummary()
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
</style>

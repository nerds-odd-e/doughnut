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
    v-if="understandingPoints.length > 0"
    class="daisy-mb-4 daisy-rounded-lg daisy-bg-accent daisy-p-4"
  >
    <div class="daisy-text-base">
      <div class="daisy-font-semibold daisy-mb-3 daisy-text-accent-content">
        Understanding Checklist:
      </div>
      <ul class="daisy-list-disc daisy-list-inside daisy-space-y-2">
        <li
          v-for="(point, index) in understandingPoints"
          :key="index"
          class="daisy-text-accent-content"
        >
          <input
            type="checkbox"
            class="daisy-checkbox daisy-checkbox-xs daisy-checkbox-accent"
            :checked="selectedPointsToRemove.has(index)"
            @change="togglePointSelection(index)"
          />
          {{ point }}
        </li>
      </ul>
      <input type="button" class="daisy-btn daisy-btn-xs daisy-btn-accent" id="rephrase-note" value="Rephrase Note" @click="handleRephraseNote" />
      <div v-if="isNoteRephrased" class="daisy-mt-3">
        <span class="daisy-font-semibold">Note Rephrased</span>
      </div>
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
const isNoteRephrased = ref(false)
const selectedPointsToRemove = ref<Set<number>>(new Set())

const togglePointSelection = (index: number) => {
  if (selectedPointsToRemove.value.has(index)) {
    selectedPointsToRemove.value.delete(index)
  } else {
    selectedPointsToRemove.value.add(index)
  }
}

const handleRephraseNote = async () => {
  // Get the selected points to remove
  const pointsToRemove = Array.from(selectedPointsToRemove.value)
    .map((index) => understandingPoints.value[index])
    .join("; ")

  if (!pointsToRemove) return

  try {
    const result = await apiCallWithLoading(() =>
      AiController.removePointFromNote({
        path: { note: note.id },
        body: pointsToRemove,
      })
    )

    if (!result.error && result.data) {
      isNoteRephrased.value = true
    }
  } catch (err) {
    console.error("Failed to rephrase note:", err)
  }
}

// Understanding checklist from backend
const understandingPoints = ref<string[]>([])
const isLoadingChecklist = ref(false)

const generateUnderstandingChecklist = async () => {
  if (!note.details || note.details.trim().length === 0) {
    understandingPoints.value = []
    return
  }

  isLoadingChecklist.value = true
  try {
    const result = await apiCallWithLoading(() =>
      AiController.generateUnderstandingChecklist({
        path: { note: note.id },
      })
    )

    if (!result.error && result.data) {
      understandingPoints.value = result.data.points || []
    } else {
      understandingPoints.value = []
    }
  } catch (err) {
    console.error("Failed to generate understanding checklist:", err)
    understandingPoints.value = []
  } finally {
    isLoadingChecklist.value = false
  }
}

const onNoteInfoLoaded = () => {
  generateUnderstandingChecklist()
}

const onNoteTypeUpdated = () => {
  generateUnderstandingChecklist()
}

const getIgnoredChecklistTopics = (): string => {
  return Array.from(selectedPointsToRemove.value)
    .map((index) => understandingPoints.value[index])
    .filter((point): point is string => Boolean(point))
    .join(", ")
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
        ignoredChecklistTopics: getIgnoredChecklistTopics(),
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

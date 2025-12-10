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
    <Select
      v-model="selectedNoteType"
      :options="noteTypeOptions"
      scope-name="note"
      field="noteType"
      @update:model-value="updateNoteType"
    />
  </div>
  <div
    v-if="noteSummaryPoints.length > 0"
    data-test="note-details-summary"
    class="daisy-mb-4 daisy-alert daisy-alert-info"
  >
    <div class="daisy-text-sm">
      <div class="daisy-font-semibold daisy-mb-2">Summary:</div>
      <ul class="daisy-list-disc daisy-list-inside daisy-space-y-1">
        <li
          v-for="(point, index) in noteSummaryPoints"
          :key="index"
          class="daisy-text-base-content/80"
        >
          {{ point }}
        </li>
      </ul>
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
import Select from "../form/Select.vue"
import { noteTypeOptions } from "@/models/noteTypeOptions"
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
const selectedNoteType = ref<NoteType>(note.noteType || "unassigned")

const showNoteTypeSelection = ref(true)

const buttonKey = computed(() => note.id)

// Summary from backend
const noteSummaryPoints = ref<string[]>([])
const isLoadingSummary = ref(false)

const generateSummary = async () => {
  if (!note.details || note.details.trim().length === 0) {
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

// Generate summary when note changes
watch(
  () => note.id,
  () => {
    generateSummary()
  },
  { immediate: true }
)

watch(
  () => note.noteType,
  (newNoteType) => {
    selectedNoteType.value = newNoteType || "unassigned"
  }
)

const updateNoteType = async (newType: NoteType) => {
  const previousValue = note.noteType || "unassigned"
  selectedNoteType.value = newType

  if (newType === note.noteType) {
    return
  }

  const { error } = await apiCallWithLoading(() =>
    NoteController.updateNoteType({
      path: { note: note.id },
      body: newType,
    })
  )

  if (error) {
    selectedNoteType.value = previousValue
  }
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

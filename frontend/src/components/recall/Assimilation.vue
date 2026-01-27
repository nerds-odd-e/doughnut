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
          class="daisy-text-accent-content daisy-flex daisy-items-center daisy-justify-between"
        >
          <span>{{ point }}</span>
          <button
            class="daisy-btn daisy-btn-xs daisy-btn-ghost"
            @click="promotePointToChildNote(point, index)"
            title="Promote to child note"
            aria-label="Promote to child note"
          >
            <SvgAdd />
          </button>
        </li>
      </ul>
    </div>
  </div>
  <AssimilationButtons
    :key="buttonKey"
    @assimilate="processForm"
  />
  <SpellingVerificationPopup
    :show="showSpellingPopup"
    :expected-title="note.noteTopology.title ?? ''"
    @cancel="handleSpellingCancel"
    @verified="handleSpellingVerified"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import {
  AiController,
  AssimilationController,
  NoteCreationController,
} from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import NoteShow from "../notes/NoteShow.vue"
import Breadcrumb from "../toolbars/Breadcrumb.vue"
import SvgAdd from "../svgs/SvgAdd.vue"
import SpellingVerificationPopup from "./SpellingVerificationPopup.vue"
import { computed, ref } from "vue"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

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
const storageAccessor = useStorageAccessor()

// State
const buttonKey = computed(() => note.id)
const showSpellingPopup = ref(false)
const rememberSpelling = ref(false)
const currentDetails = ref(note.details)

const onDetailsSaved = (newDetails: string) => {
  currentDetails.value = newDetails
}

// Understanding checklist from backend
const understandingPoints = ref<string[]>([])

const generateUnderstandingChecklist = async () => {
  if (!currentDetails.value || currentDetails.value.trim().length === 0) {
    understandingPoints.value = []
    return
  }

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
  }
}

const onNoteInfoLoaded = () => {
  generateUnderstandingChecklist()
}

const onRememberSpellingChanged = (value: boolean) => {
  rememberSpelling.value = value
}

const onNoteTypeUpdated = () => {
  generateUnderstandingChecklist()
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

const promotePointToChildNote = async (point: string, index: number) => {
  try {
    const { data: nrwp, error } = await apiCallWithLoading(() =>
      NoteCreationController.createNoteUnderParent({
        path: { parentNote: note.id },
        body: { newTitle: point, wikidataId: "" },
      })
    )

    if (error || !nrwp) {
      await popups.alert("Failed to create child note")
      return
    }

    // 手动更新 storage（不导航）
    if (storageAccessor.value) {
      storageAccessor.value.refreshNoteRealm(nrwp.created)
      storageAccessor.value.refreshNoteRealm(nrwp.parent)
    }

    // 从列表中移除该 point
    understandingPoints.value.splice(index, 1)
  } catch (err) {
    console.error("Failed to promote point to child note:", err)
    await popups.alert("Error creating child note")
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

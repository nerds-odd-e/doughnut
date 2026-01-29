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
    data-test-id="understanding-checklist"
  >
    <div class="daisy-text-base">
      <div class="daisy-font-semibold daisy-mb-3 daisy-text-accent-content">
        Understanding Checklist:
      </div>
      <ul class="daisy-space-y-2">
        <li
          v-for="(point, index) in understandingPoints"
          :key="index"
          class="daisy-text-accent-content daisy-flex daisy-items-start daisy-gap-2"
        >
          <label class="daisy-flex daisy-items-start daisy-cursor-pointer daisy-gap-2 daisy-flex-1 daisy-min-w-0">
            <input
              type="checkbox"
              :value="index"
              v-model="selectedPointIndices"
              class="daisy-checkbox daisy-checkbox-accent daisy-checkbox-sm daisy-mt-1 daisy-border-black dark:daisy-border-white daisy-border-2 daisy-shrink-0"
            />
            <span class="daisy-break-words">{{ point }}</span>
          </label>
          <div class="daisy-flex daisy-gap-1 daisy-shrink-0">
            <button
              class="daisy-btn daisy-btn-xs daisy-btn-ghost"
              @click="promotePointToChildNote(point, index)"
              title="Promote to child note"
            >
              <SvgAddChild class="w-4 h-4" />
              Child
            </button>
            <button
              v-if="note.parentId"
              class="daisy-btn daisy-btn-xs daisy-btn-ghost"
              @click="promotePointToSiblingNote(point, index)"
              title="Promote to sibling note"
            >
              <SvgAddSibling class="w-4 h-4" />
              Sibling
            </button>
          </div>
        </li>
      </ul>
      <button
        data-test-id="delete-understanding-points"
        :disabled="selectedPointIndices.length === 0"
        @click="deleteSelectedPoints"
        class="daisy-btn daisy-btn-error daisy-btn-sm daisy-mt-4 !daisy-text-white"
      >
        Delete selected points
      </button>
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
    @add-answer="handleAddAnswer"
  />
  <LoadingModal :show="isPromotingPoint" message="AI is creating note..." />
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import {
  AiController,
  AssimilationController,
  TextContentController,
} from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import AssimilationButtons from "./AssimilationButtons.vue"
import NoteShow from "../notes/NoteShow.vue"
import Breadcrumb from "../toolbars/Breadcrumb.vue"
import SpellingVerificationPopup from "./SpellingVerificationPopup.vue"
import SvgAddChild from "../svgs/SvgAddChild.vue"
import SvgAddSibling from "../svgs/SvgAddSibling.vue"
import LoadingModal from "../commons/LoadingModal.vue"
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
const isPromotingPoint = ref(false)

const onDetailsSaved = (newDetails: string) => {
  currentDetails.value = newDetails
}

// Understanding checklist from backend
const understandingPoints = ref<string[]>([])
const selectedPointIndices = ref<number[]>([])

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

const deleteSelectedPoints = async () => {
  if (selectedPointIndices.value.length === 0) {
    return
  }

  const confirmed = await popups.confirm(
    `Are you sure you want to delete ${selectedPointIndices.value.length} selected point(s)? The AI will remove related content from the note details.`
  )

  if (!confirmed) {
    return
  }

  const selectedPoints = selectedPointIndices.value.map(
    (index) => understandingPoints.value[index]!
  )

  const { error } = await apiCallWithLoading(() =>
    AiController.removePointFromNote({
      path: { note: note.id },
      body: { points: selectedPoints },
    })
  )

  if (!error) {
    emit("reloadNeeded")
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

const handleAddAnswer = async (answer: string) => {
  const currentTitle = note.noteTopology.title ?? ""
  const newTitle = `${currentTitle} / ${answer}`

  const { error, data } = await apiCallWithLoading(() =>
    TextContentController.updateNoteTitle({
      path: { note: note.id },
      body: { newTitle },
    })
  )

  if (!error && data) {
    // Update storage cache manually
    if (storageAccessor.value) {
      storageAccessor.value.refreshNoteRealm(data)
    }
    showSpellingPopup.value = false
    emit("reloadNeeded")
  }
}

const promotePoint = async (
  point: string,
  index: number,
  parentNoteId: number
) => {
  isPromotingPoint.value = true
  try {
    // Call AI endpoint to promote point to a new note
    const { data: result, error } = await apiCallWithLoading(() =>
      AiController.promotePoint({
        path: { note: note.id },
        body: { point, parentNoteId },
      })
    )

    if (error || !result || !result.createdNote || !result.updatedParentNote) {
      await popups.alert("Failed to create note with AI")
      return
    }

    // Extract non-null values for type safety
    const createdNote = result.createdNote
    const updatedParentNote = result.updatedParentNote

    // Update storage (including parent note which was updated by AI)
    if (storageAccessor.value) {
      storageAccessor.value.refreshNoteRealm(createdNote)
      storageAccessor.value.refreshNoteRealm(updatedParentNote)
    }

    // Remove the point from the list
    understandingPoints.value.splice(index, 1)
  } catch (err) {
    console.error("Failed to promote point:", err)
    await popups.alert(`Error: ${err}`)
  } finally {
    isPromotingPoint.value = false
  }
}

const promotePointToChildNote = (point: string, index: number) => {
  // Create as child: use current note as parent (pass note.id)
  promotePoint(point, index, note.id)
}

const promotePointToSiblingNote = (point: string, index: number) => {
  // Create as sibling: use current note's parent as parent
  // note.parentId is guaranteed to exist because button is disabled when !note.parentId
  promotePoint(point, index, note.parentId!)
}
</script>

<style>
.initial-review-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

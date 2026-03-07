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
    @remember-spelling-changed="onRememberSpellingChanged"
    @note-recall-info-loaded="onNoteRecallInfoLoaded"
  />
  <NoteRefinement
    v-if="(note.details ?? '').trim()"
    :note="note"
    @details-updated="$emit('reloadNeeded')"
  />
  <AssimilationButtons
    :key="buttonKey"
    :disabled="!noteInfoLoaded"
    :keep-for-repetition-disabled="keepForRepetitionDisabled"
    @assimilate="processForm"
  />
  <Teleport to="body">
    <div
      v-if="showSpellingPopup"
      data-test="opaque-content-blocker"
      class="fixed inset-0 daisy-bg-black"
      style="z-index: 9989"
      aria-hidden="true"
    />
  </Teleport>
  <SpellingVerificationPopup
    :show="showSpellingPopup"
    :note-id="note.id"
    @cancel="handleSpellingCancel"
    @verified="handleSpellingVerified"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { AssimilationController } from "@generated/doughnut-backend-api/sdk.gen"
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
  (e: "assimilationDone"): void
}>()

// Composables
const { popups } = usePopups()
const { totalAssimilatedCount } = useRecallData()

const { incrementAssimilatedCount } = useAssimilationCount()

// State
const buttonKey = computed(() => note.id)
const showSpellingPopup = ref(false)
const rememberSpelling = ref(false)
const noteInfoLoaded = ref(false)
const noteRecallInfo = ref<{
  memoryTrackers?: Array<{ spelling?: boolean }>
} | null>(null)

const onRememberSpellingChanged = (value: boolean) => {
  rememberSpelling.value = value
  noteInfoLoaded.value = true
}

const onNoteRecallInfoLoaded = (info: {
  memoryTrackers?: Array<{ spelling?: boolean }>
}) => {
  noteRecallInfo.value = info
  noteInfoLoaded.value = true
}

const hasMemoryTrackers = computed(
  () => (noteRecallInfo.value?.memoryTrackers?.length ?? 0) > 0
)
const hasSpellingMemoryTracker = computed(
  () =>
    noteRecallInfo.value?.memoryTrackers?.some((mt) => mt.spelling === true) ??
    false
)
<<<<<<< HEAD
=======
const addSpellingOnlyMode = computed(
  () =>
    hasMemoryTrackers.value &&
    rememberSpelling.value &&
    !hasSpellingMemoryTracker.value
)
>>>>>>> 8873fd737 (Disable keep for repetition when note has memory trackers; add spelling-only flow)
const keepForRepetitionDisabled = computed(
  () =>
    hasMemoryTrackers.value &&
    !(rememberSpelling.value && !hasSpellingMemoryTracker.value)
)

// Methods
const processForm = async (skipMemoryTracking: boolean) => {
  if (skipMemoryTracking) {
    const confirmed = await popups.confirm(
      "Confirm to hide this note from recalls in the future?"
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

const doAssimilate = async (
  skipMemoryTracking: boolean,
  addSpellingOnly = false
) => {
  const { data: memoryTrackers, error } = await apiCallWithLoading(() =>
    AssimilationController.assimilate({
      body: {
        noteId: note.id,
        skipMemoryTracking,
        addSpellingOnly: addSpellingOnly || undefined,
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
      emit("assimilationDone")
    }
  }
}

const handleSpellingVerified = () => {
  showSpellingPopup.value = false
  doAssimilate(false, addSpellingOnlyMode.value)
}

const handleSpellingCancel = () => {
  showSpellingPopup.value = false
}
</script>

<style>
.assimilation-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

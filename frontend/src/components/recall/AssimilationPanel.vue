<template>
  <AssimilationSettings
    :note="note"
    :note-info-loaded="noteInfoLoaded"
    :keep-for-recall-disabled="keepForRecallDisabled"
    @level-changed="emit('reloadNeeded')"
    @remember-spelling-changed="onRememberSpellingChanged"
    @note-recall-info-loaded="onNoteRecallInfoLoaded"
    @assimilate="processForm"
    @refinement-content-updated="emit('reloadNeeded')"
  />
  <Teleport to="body">
    <div
      v-if="showSpellingPopup"
      data-test="opaque-content-blocker"
      class="fixed inset-0 bg-black"
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
import AssimilationSettings from "./AssimilationSettings.vue"
import SpellingVerificationPopup from "./SpellingVerificationPopup.vue"
import { computed, ref } from "vue"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"

const { note } = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "reloadNeeded"): void
}>()

const { popups } = usePopups()
const { totalAssimilatedCount, requestDueRecallsRefresh } = useRecallData()
const { goToNextAssimilation } = useGoToNextAssimilation()

const { incrementAssimilatedCount } = useAssimilationCount()

const showSpellingPopup = ref(false)

const rememberSpelling = ref(false)
const noteInfoLoaded = ref(false)
const noteRecallInfo = ref<{
  memoryTrackers?: Array<{ spelling?: boolean }>
} | null>(null)

const onRememberSpellingChanged = (value: boolean) => {
  rememberSpelling.value = value
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
const keepForRecallDisabled = computed(
  () =>
    hasMemoryTrackers.value &&
    !(rememberSpelling.value && !hasSpellingMemoryTracker.value)
)

const processForm = async (skipMemoryTracking: boolean) => {
  if (skipMemoryTracking) {
    const confirmed = await popups.confirm(
      "Confirm to hide this note from recalls in the future?"
    )
    if (!confirmed) {
      return
    }
  }

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

    requestDueRecallsRefresh()

    const navigated = await goToNextAssimilation()
    if (!navigated) {
      emit("reloadNeeded")
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

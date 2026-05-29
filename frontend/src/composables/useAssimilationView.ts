import { ref } from "vue"

const showAssimilationSettings = ref(false)
const pendingOnForNoteId = ref<number | null>(null)

export function useAssimilationView() {
  const isOnForNote = (noteId: number) =>
    showAssimilationSettings.value && pendingOnForNoteId.value === noteId

  const requestOnFor = (noteId: number) => {
    pendingOnForNoteId.value = noteId
    showAssimilationSettings.value = true
  }

  const resetForNote = (noteId: number) => {
    showAssimilationSettings.value = pendingOnForNoteId.value === noteId
  }

  const toggle = (noteId: number) => {
    if (showAssimilationSettings.value && pendingOnForNoteId.value === noteId) {
      pendingOnForNoteId.value = null
      showAssimilationSettings.value = false
      return
    }
    requestOnFor(noteId)
  }

  return {
    showAssimilationSettings,
    pendingOnForNoteId,
    isOnForNote,
    requestOnFor,
    resetForNote,
    toggle,
  }
}

/** @internal test-only reset of module singleton state */
export function resetAssimilationViewForTests() {
  showAssimilationSettings.value = false
  pendingOnForNoteId.value = null
}

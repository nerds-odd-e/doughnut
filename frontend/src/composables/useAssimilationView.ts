import { ref } from "vue"

const showAssimilationSettings = ref(false)
const pendingOnForNoteId = ref<number | null>(null)

function clearAssimilationView() {
  pendingOnForNoteId.value = null
  showAssimilationSettings.value = false
}

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

  const dismiss = () => {
    clearAssimilationView()
  }

  const toggle = (noteId: number) => {
    if (showAssimilationSettings.value && pendingOnForNoteId.value === noteId) {
      dismiss()
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
    dismiss,
    toggle,
  }
}

/** @internal test-only reset of module singleton state */
export function resetAssimilationViewForTests() {
  clearAssimilationView()
}

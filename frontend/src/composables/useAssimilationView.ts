import { ref } from "vue"

const showAssimilationSettings = ref(false)
const targetNoteId = ref<number | null>(null)
const pendingPropertyKey = ref<string | null>(null)

function clearAssimilationView() {
  targetNoteId.value = null
  pendingPropertyKey.value = null
  showAssimilationSettings.value = false
}

export function useAssimilationView() {
  const isOpenForNote = (noteId: number) =>
    showAssimilationSettings.value && targetNoteId.value === noteId

  const openForNote = (noteId: number, propertyKey?: string | null) => {
    targetNoteId.value = noteId
    pendingPropertyKey.value = propertyKey ?? null
    showAssimilationSettings.value = true
  }

  const resetForNote = (noteId: number) => {
    showAssimilationSettings.value = targetNoteId.value === noteId
  }

  const dismiss = () => {
    clearAssimilationView()
  }

  const toggle = (noteId: number) => {
    if (showAssimilationSettings.value && targetNoteId.value === noteId) {
      dismiss()
      return
    }
    openForNote(noteId)
  }

  return {
    showAssimilationSettings,
    targetNoteId,
    pendingPropertyKey,
    isOpenForNote,
    openForNote,
    resetForNote,
    dismiss,
    toggle,
  }
}

import { ref } from "vue"

const targetNoteId = ref<number | null>(null)
const showAssimilationPanel = ref(false)

export function useAssimilationView() {
  const isOpenForNote = (noteId: number) =>
    showAssimilationPanel.value && targetNoteId.value === noteId

  const openForNote = (noteId: number) => {
    targetNoteId.value = noteId
    showAssimilationPanel.value = true
  }

  const resetForNote = (noteId: number) => {
    showAssimilationPanel.value = targetNoteId.value === noteId
  }

  const dismiss = () => {
    targetNoteId.value = null
    showAssimilationPanel.value = false
  }

  const toggle = (noteId: number) => {
    if (isOpenForNote(noteId)) {
      dismiss()
      return
    }
    openForNote(noteId)
  }

  return {
    showAssimilationPanel,
    targetNoteId,
    isOpenForNote,
    openForNote,
    resetForNote,
    dismiss,
    toggle,
  }
}

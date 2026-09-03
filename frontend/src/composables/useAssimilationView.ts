import { computed, ref } from "vue"
import { useNoteToolbarPanel } from "./useNoteToolbarPanel"

const targetNoteId = ref<number | null>(null)

export function useAssimilationView() {
  const { activePanel, close: closePanel } = useNoteToolbarPanel()

  const showAssimilationPanel = computed(
    () => activePanel.value === "assimilation"
  )

  const isOpenForNote = (noteId: number) =>
    showAssimilationPanel.value && targetNoteId.value === noteId

  const closeAssimilationPanelIfOpen = () => {
    if (showAssimilationPanel.value) {
      closePanel()
    }
  }

  const openForNote = (noteId: number) => {
    targetNoteId.value = noteId
    activePanel.value = "assimilation"
  }

  const resetForNote = (noteId: number) => {
    if (targetNoteId.value === noteId) {
      activePanel.value = "assimilation"
      return
    }
    closeAssimilationPanelIfOpen()
  }

  const dismiss = () => {
    targetNoteId.value = null
    closeAssimilationPanelIfOpen()
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

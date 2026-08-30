import { computed, ref } from "vue"
import { useNoteToolbarPanel } from "./useNoteToolbarPanel"

const targetNoteId = ref<number | null>(null)

export function useAssimilationView() {
  const { activePanel, close: closePanel } = useNoteToolbarPanel()

  const showAssimilationSettings = computed(
    () => activePanel.value === "assimilation"
  )

  const isOpenForNote = (noteId: number) =>
    showAssimilationSettings.value && targetNoteId.value === noteId

  const closeAssimilationSettingsIfOpen = () => {
    if (showAssimilationSettings.value) {
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
    closeAssimilationSettingsIfOpen()
  }

  const dismiss = () => {
    targetNoteId.value = null
    closeAssimilationSettingsIfOpen()
  }

  const toggle = (noteId: number) => {
    if (isOpenForNote(noteId)) {
      dismiss()
      return
    }
    openForNote(noteId)
  }

  return {
    showAssimilationSettings,
    targetNoteId,
    isOpenForNote,
    openForNote,
    resetForNote,
    dismiss,
    toggle,
  }
}

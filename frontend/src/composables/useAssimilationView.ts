import { computed, ref } from "vue"
import { useNoteToolbarPanel } from "./useNoteToolbarPanel"

const targetNoteId = ref<number | null>(null)
const pendingPropertyKey = ref<string | null>(null)

function clearAssimilationTargets() {
  targetNoteId.value = null
  pendingPropertyKey.value = null
}

export function useAssimilationView() {
  const { activePanel, close: closePanel } = useNoteToolbarPanel()

  const showAssimilationSettings = computed(
    () => activePanel.value === "assimilation"
  )

  const isOpenForNote = (noteId: number) =>
    showAssimilationSettings.value && targetNoteId.value === noteId

  const openForNote = (noteId: number, propertyKey?: string | null) => {
    targetNoteId.value = noteId
    pendingPropertyKey.value = propertyKey ?? null
    activePanel.value = "assimilation"
  }

  const resetForNote = (noteId: number) => {
    if (targetNoteId.value === noteId) {
      activePanel.value = "assimilation"
      return
    }
    if (activePanel.value === "assimilation") {
      closePanel()
    }
  }

  const dismiss = () => {
    clearAssimilationTargets()
    if (activePanel.value === "assimilation") {
      closePanel()
    }
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
    pendingPropertyKey,
    isOpenForNote,
    openForNote,
    resetForNote,
    dismiss,
    toggle,
  }
}

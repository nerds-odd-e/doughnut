import { computed, ref } from "vue"

export type NoteToolbarPanelId = "none" | "audio" | "assimilation"

const activePanel = ref<NoteToolbarPanelId>("none")

export function useNoteToolbarPanel() {
  const isAudioOpen = computed(() => activePanel.value === "audio")
  const isPanelOpen = computed(() => activePanel.value !== "none")

  const toggleAudio = () => {
    activePanel.value = activePanel.value === "audio" ? "none" : "audio"
  }

  const close = () => {
    activePanel.value = "none"
  }

  return {
    activePanel,
    isAudioOpen,
    isPanelOpen,
    toggleAudio,
    close,
  }
}

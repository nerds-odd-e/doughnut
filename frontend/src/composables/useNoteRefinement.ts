import type { Note } from "@generated/doughnut-backend-api"
import { useNoteExtractionPreview } from "@/composables/useNoteExtractionPreview"
import { useNoteRefinementLayout } from "@/composables/useNoteRefinementLayout"
import { type Ref } from "vue"

export function useNoteRefinement(
  note: Ref<Note>,
  onContentUpdated: (newContent: string) => void
) {
  let onLayoutReset: () => void = () => undefined

  const layout = useNoteRefinementLayout(note, onContentUpdated, () =>
    onLayoutReset()
  )

  const extraction = useNoteExtractionPreview(note, layout.layoutSelectionBody)
  onLayoutReset = extraction.resetExtractionPreview

  return {
    refinementLayoutItems: layout.refinementLayoutItems,
    layoutLoadSettled: layout.layoutLoadSettled,
    showExportExtractDialog: layout.showExportExtractDialog,
    showExportBreakdownDialog: layout.showExportBreakdownDialog,
    selectedItemIds: layout.selectedItemIds,
    isFullySelected: layout.isFullySelected,
    isPartiallySelected: layout.isPartiallySelected,
    setItemSelection: layout.setItemSelection,
    loadRefinementLayout: layout.loadRefinementLayout,
    removeSelectedLayoutItems: layout.removeSelectedLayoutItems,
    fetchExtractRequestExport: layout.fetchExtractRequestExport,
    fetchBreakdownRequestExport: layout.fetchBreakdownRequestExport,
    showExtractionPreview: extraction.showExtractionPreview,
    extractionPreview: extraction.extractionPreview,
    createError: extraction.createError,
    canCreateExtractedNote: extraction.canCreateExtractedNote,
    openExtractionPreview: extraction.openExtractionPreview,
    retryExtractionPreview: extraction.retryExtractionPreview,
    backToLayout: extraction.backToLayout,
    createExtractedNote: extraction.createExtractedNote,
  }
}

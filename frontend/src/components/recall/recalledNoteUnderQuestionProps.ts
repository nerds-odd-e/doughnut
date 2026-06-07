import type { RecalledNote } from "@generated/doughnut-backend-api"

export function recalledNoteUnderQuestionProps(recalledNote: RecalledNote) {
  return {
    noteTopology: recalledNote.noteTopology,
    ancestorFolders: recalledNote.ancestorFolders ?? [],
    breadcrumbNotebookId: recalledNote.notebookId,
    focusedPropertyKey: recalledNote.propertyKey,
  }
}

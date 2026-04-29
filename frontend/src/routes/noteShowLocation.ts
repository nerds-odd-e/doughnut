import type { NoteRealm, NoteTopology } from "@generated/doughnut-backend-api"
import type { RouteLocationNamedRaw } from "vue-router"

export function noteShowByNotebookSlugLocation(
  notebookId: number,
  noteSlugPath: string
): RouteLocationNamedRaw {
  return {
    name: "noteShowByNotebookSlug",
    params: {
      notebookId: String(notebookId),
      noteSlugPath,
    },
  }
}

export function noteShowByNotebookSlugLocationFromNoteRealm(
  noteRealm: NoteRealm
): RouteLocationNamedRaw {
  return noteShowByNotebookSlugLocation(noteRealm.notebook.id, noteRealm.slug)
}

export function noteShowByNotebookSlugLocationFromNoteTopology(
  noteTopology: NoteTopology
): RouteLocationNamedRaw {
  return noteShowByNotebookSlugLocation(
    noteTopology.notebookId,
    noteTopology.slug
  )
}

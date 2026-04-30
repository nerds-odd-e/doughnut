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

/** Plain path for wiki links in HTML (matches `noteShowByNotebookSlug`). */
export function noteShowByNotebookSlugHref(
  notebookId: number,
  noteSlugPath: string
): string {
  const tail =
    noteSlugPath === ""
      ? ""
      : noteSlugPath
          .split("/")
          .map((seg) => encodeURIComponent(seg))
          .join("/")
  return `/d/notebooks/${notebookId}/notes/${tail}`
}

export function noteShowByNotebookSlugLocationFromNoteRealm(
  noteRealm: NoteRealm
): RouteLocationNamedRaw {
  return noteShowByNotebookSlugLocation(noteRealm.notebookId, noteRealm.slug)
}

export function noteShowByNotebookSlugLocationFromNoteTopology(
  noteTopology: NoteTopology
): RouteLocationNamedRaw {
  return noteShowByNotebookSlugLocation(
    noteTopology.notebookId,
    noteTopology.slug
  )
}

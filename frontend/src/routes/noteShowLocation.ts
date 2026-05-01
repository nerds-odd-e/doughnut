import type { NoteRealm, NoteTopology } from "@generated/doughnut-backend-api"
import type { RouteLocationNamedRaw } from "vue-router"

export function noteShowLocation(noteId: number): RouteLocationNamedRaw {
  return {
    name: "noteShow",
    params: {
      noteId: String(noteId),
    },
  }
}

export function noteShowHref(noteId: number): string {
  return `/d/n/${noteId}`
}

/** Plain path for wiki links in HTML until WikiTitle exposes note id (Phase 2). */
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

export function noteShowLegacyNotebookSlugLocation(
  notebookId: number,
  noteSlugPath: string
): RouteLocationNamedRaw {
  return {
    name: "noteShowLegacyNotebookSlug",
    params: {
      notebookId: String(notebookId),
      noteSlugPath,
    },
  }
}

export function noteShowLocationFromNoteRealm(
  noteRealm: NoteRealm
): RouteLocationNamedRaw {
  return noteShowLocation(noteRealm.id)
}

export function noteShowLocationFromNoteTopology(
  noteTopology: NoteTopology
): RouteLocationNamedRaw {
  return noteShowLocation(noteTopology.id)
}

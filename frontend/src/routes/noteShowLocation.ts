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

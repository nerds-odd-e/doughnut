import type {
  FolderTrailSegment,
  NotebookRealm,
  NoteTopology,
} from "@generated/donut-backend-api"
import type { RouteLocationNamedRaw } from "vue-router"
import { noteShowLocation } from "./noteShowLocation"

/** Where the reader lands once the note, folder or file they were viewing is gone. */
export function containingLocationOf(realm: {
  notebookRealm: NotebookRealm
  ancestorFolders?: FolderTrailSegment[]
}): RouteLocationNamedRaw {
  const notebookId = realm.notebookRealm.notebook.id
  const parent = realm.ancestorFolders?.at(-1)
  if (!parent) return { name: "notebookPage", params: { notebookId } }
  return {
    name: "folderPage",
    params: { notebookId: String(notebookId), folderId: String(parent.id) },
  }
}

/** Where the reader lands once a note is gone: its neighboring note, else its containing location. */
export function locationAfterNoteRemoval(
  realm: Parameters<typeof containingLocationOf>[0],
  neighbor: NoteTopology | undefined
): RouteLocationNamedRaw {
  return neighbor ? noteShowLocation(neighbor.id) : containingLocationOf(realm)
}

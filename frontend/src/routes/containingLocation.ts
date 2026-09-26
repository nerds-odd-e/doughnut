import type { Folder, NotebookRealm } from "@generated/donut-backend-api"
import type { RouteLocationNamedRaw } from "vue-router"

/** Where the reader lands once the folder or file they were viewing is gone. */
export function containingLocationOf(realm: {
  notebookRealm: NotebookRealm
  ancestorFolders?: Folder[]
}): RouteLocationNamedRaw {
  const notebookId = realm.notebookRealm.notebook.id
  const parent = realm.ancestorFolders?.at(-1)
  if (!parent) return { name: "notebookPage", params: { notebookId } }
  return {
    name: "folderPage",
    params: { notebookId: String(notebookId), folderId: String(parent.id) },
  }
}

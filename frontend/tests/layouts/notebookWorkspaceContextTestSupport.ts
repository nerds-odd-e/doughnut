import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import {
  atViewportWidth,
  mountNotebookWorkspace,
} from "./notebookWorkspaceHeaderTestSupport"

export const longNotebook = "An exceptionally long notebook name about orchards"
export const parentFolder = {
  id: 501,
  name: "Fruit trees of the north",
}
export const innerFolder = {
  id: 502,
  name: "Apples pears and quinces grown on dwarfing rootstocks",
}

export function deepNote() {
  return makeMe.aNoteRealm
    .title("Orchard")
    .notebookName(longNotebook)
    .ancestorFolders([parentFolder, innerFolder])
    .please()
}

export const notebookLocationControl = () =>
  document.querySelector<HTMLElement>(".notebook-location")!
export async function mountDeepNote(width: number) {
  await atViewportWidth(width)
  const realm = deepNote()
  const mounted = await mountNotebookWorkspace([realm])
  useNoteStore().noteUndo.addEditingToUndoHistory(realm.id, "edit title", "Old")
  await flushPromises()
  return { ...mounted, realm }
}

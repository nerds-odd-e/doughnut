import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { testFolderStub } from "@tests/helpers"
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

export function populatedNotebook(noteTitlePrefix = "Note") {
  const notebook = deepNote().notebookRealm.notebook
  const notes = Array.from({ length: 40 }, (_, i) =>
    makeMe.aNoteRealm
      .title(`${noteTitlePrefix} ${String(i + 1).padStart(2, "0")}`)
      .inNotebook(notebook.id, notebook.name)
      .ancestorFolders([parentFolder, innerFolder])
      .please()
  )
  return {
    notes,
    folderListings: {
      undefined: {
        noteTopologies: [],
        folders: [testFolderStub(parentFolder.id, parentFolder.name)],
      },
      [parentFolder.id]: {
        noteTopologies: [],
        folders: [testFolderStub(innerFolder.id, innerFolder.name)],
      },
      [innerFolder.id]: {
        noteTopologies: notes.map((note) => note.note.noteTopology),
        folders: [],
      },
    },
  }
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

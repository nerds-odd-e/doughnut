import type { FolderListing, NoteRealm } from "@generated/donut-backend-api"
import {
  NoteController,
  NotebookFolderController,
} from "@generated/donut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { PEER_SORT_STORAGE_KEY } from "@/composables/usePeerSort"
import { noteShowLocation } from "@/routes/noteShowLocation"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, testFolderStub } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("where the person lands once a note is removed", () => {
  const routerReplace = vi.fn()
  const router = { replace: routerReplace } as unknown as Router
  const notebookId = 71
  const peer = (title: string) =>
    makeMe.aNoteRealm
      .title(title)
      .inNotebook(notebookId)
      .inFolder(901, "Work")
      .please()
  const [a, b, c] = [peer("A"), peer("B"), peer("C")]

  type Storage = ReturnType<typeof createNoteStorage>
  type Removal = (storage: Storage, id: number) => Promise<unknown>
  const trash: Removal = (storage, id) =>
    storage.storedApi().trashNote(router, id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })
  const permanentlyDelete: Removal = (storage, id) =>
    storage.storedApi().permanentlyDeleteNote(router, id)

  const removeWithPeers = async (
    remove: Removal,
    realm: NoteRealm,
    listing: FolderListing
  ) => {
    const storage = createNoteStorage()
    storage.refreshNoteRealm(realm)
    const listingSpy = mockSdkService(
      NotebookFolderController,
      "listNotebookFolderListing",
      listing
    )
    mockSdkService(NoteController, "trashNote", realm)
    mockSdkService(NoteController, "permanentlyDeleteNote", undefined)
    await remove(storage, realm.id)
    return listingSpy
  }

  const notesListing = (...realms: NoteRealm[]): FolderListing => ({
    noteTopologies: realms.map((r) => r.note.noteTopology),
  })

  beforeEach(() => vi.clearAllMocks())
  afterEach(() => localStorage.removeItem(PEER_SORT_STORAGE_KEY))

  describe.each([
    { removal: "trashing", remove: trash },
    { removal: "permanently deleting", remove: permanentlyDelete },
  ])("after $removal", ({ remove }) => {
    it.each([
      { removed: b, expected: c },
      { removed: c, expected: b },
    ])(
      "opens the neighboring note: $removed.note.noteTopology.title → $expected.note.noteTopology.title",
      async ({ removed, expected }) => {
        const listingSpy = await removeWithPeers(
          remove,
          removed,
          notesListing(c, a, b)
        )

        expect(listingSpy).toHaveBeenCalledWith({
          path: { notebook: notebookId },
          query: { parent: 901 },
        })
        expect(routerReplace).toHaveBeenCalledWith(
          noteShowLocation(expected.id)
        )
      }
    )

    it("follows the sidebar order chosen in this browser", async () => {
      localStorage.setItem(
        PEER_SORT_STORAGE_KEY,
        JSON.stringify({ field: "title", direction: "desc" })
      )

      await removeWithPeers(remove, b, notesListing(a, b, c))

      expect(routerReplace).toHaveBeenCalledWith(noteShowLocation(a.id))
    })

    it("opens the folder page when only subfolders and files remain beside the note", async () => {
      await removeWithPeers(remove, a, {
        ...notesListing(a),
        folders: [testFolderStub(902, "Sub")],
        attachments: [{ id: 5, filename: "B.png" }],
      })

      expect(routerReplace).toHaveBeenCalledWith({
        name: "folderPage",
        params: { notebookId: String(notebookId), folderId: "901" },
      })
    })
  })

  it("opens the notebook page when trashing the only note at the notebook root", async () => {
    const realm = makeMe.aNoteRealm.inNotebook(notebookId).please()

    const listingSpy = await removeWithPeers(trash, realm, notesListing(realm))

    expect(listingSpy).toHaveBeenCalledWith({
      path: { notebook: notebookId },
      query: undefined,
    })
    expect(routerReplace).toHaveBeenCalledWith({
      name: "notebookPage",
      params: { notebookId },
    })
  })
})

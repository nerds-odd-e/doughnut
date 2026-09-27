import type { FolderListing, NoteRealm } from "@generated/donut-backend-api"
import {
  NoteController,
  NotebookFolderController,
} from "@generated/donut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { sidebarStructuralRefreshKey } from "@/components/notes/sidebarStructuralRefresh"
import { PEER_SORT_STORAGE_KEY } from "@/composables/usePeerSort"
import { noteShowLocation } from "@/routes/noteShowLocation"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, testFolderStub, wrapSdkError } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("storedApiCollection trash note", () => {
  const routerReplace = vi.fn()
  const routerPush = vi.fn()
  const router = {
    replace: routerReplace,
    push: routerPush,
  } as unknown as Router

  beforeEach(() => {
    vi.clearAllMocks()
    mockSdkService(NotebookFolderController, "listNotebookFolderListing", {})
  })

  it.each([
    {
      realm: makeMe.aNoteRealm.title("Root title").please(),
      originalFolderId: null,
    },
    {
      realm: makeMe.aNoteRealm
        .title("Folder title")
        .inFolder(901, "Work")
        .please(),
      originalFolderId: 901,
    },
  ])(
    "records the original title/placement for undo ($originalFolderId)",
    async ({ realm, originalFolderId }) => {
      const storage = createNoteStorage()
      storage.refreshNoteRealm(realm)
      const trashedRealm = makeMe.aNoteRealm
        .id(realm.id)
        .title(`${realm.note.noteTopology.title} (2)`)
        .please()
      const trashSpy = mockSdkService(NoteController, "trashNote", trashedRealm)

      await storage.storedApi().trashNote(router, realm.id, {
        referenceHandling: "REMOVE_FROM_PROPERTIES",
      })

      expect(trashSpy).toHaveBeenCalledWith({
        path: { note: realm.id },
        body: { referenceHandling: "REMOVE_FROM_PROPERTIES" },
      })
      expect(storage.peekUndo()).toEqual({
        type: "trash note",
        noteId: realm.id,
        originalTitle: realm.note.noteTopology.title,
        originalFolderId,
      })
    }
  )

  describe("where the person lands", () => {
    const notebookId = 71
    const peer = (title: string) =>
      makeMe.aNoteRealm
        .title(title)
        .inNotebook(notebookId)
        .inFolder(901, "Work")
        .please()
    const [a, b, c] = [peer("A"), peer("B"), peer("C")]

    const trashWithPeers = async (realm: NoteRealm, listing: FolderListing) => {
      const storage = createNoteStorage()
      storage.refreshNoteRealm(realm)
      const listingSpy = mockSdkService(
        NotebookFolderController,
        "listNotebookFolderListing",
        listing
      )
      mockSdkService(NoteController, "trashNote", realm)
      await storage.storedApi().trashNote(router, realm.id, {
        referenceHandling: "LEAVE_DEAD_LINKS",
      })
      return listingSpy
    }

    const notesListing = (...realms: NoteRealm[]): FolderListing => ({
      noteTopologies: realms.map((r) => r.note.noteTopology),
    })

    afterEach(() => localStorage.removeItem(PEER_SORT_STORAGE_KEY))

    it.each([
      { removed: b, expected: c },
      { removed: c, expected: b },
    ])(
      "opens the neighboring note: $removed.note.noteTopology.title → $expected.note.noteTopology.title",
      async ({ removed, expected }) => {
        const listingSpy = await trashWithPeers(removed, notesListing(c, a, b))

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

      await trashWithPeers(b, notesListing(a, b, c))

      expect(routerReplace).toHaveBeenCalledWith(noteShowLocation(a.id))
    })

    it("opens the folder page when only subfolders and files remain beside the note", async () => {
      await trashWithPeers(a, {
        ...notesListing(a),
        folders: [testFolderStub(902, "Sub")],
        attachments: [{ id: 5, filename: "B.png" }],
      })

      expect(routerReplace).toHaveBeenCalledWith({
        name: "folderPage",
        params: { notebookId: String(notebookId), folderId: "901" },
      })
    })

    it("opens the notebook page when no other note is at the notebook root", async () => {
      const realm = makeMe.aNoteRealm.inNotebook(notebookId).please()

      const listingSpy = await trashWithPeers(realm, notesListing(realm))

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

  it("undoes trash atomically with the original title/placement and consumes history after success", async () => {
    const storage = createNoteStorage()
    const realm = makeMe.aNoteRealm
      .title("Original title")
      .inFolder(901, "Work")
      .please()
    storage.refreshNoteRealm(realm)
    mockSdkService(NoteController, "trashNote", realm)
    await storage.storedApi().trashNote(router, realm.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })
    const undoSpy = mockSdkService(NoteController, "undoTrashNote", realm)

    await storage.storedApi().undo(router)

    expect(undoSpy).toHaveBeenCalledWith({
      path: { note: realm.id },
      body: { priorTitle: "Original title", priorFolderId: 901 },
    })
    expect(storage.peekUndo()).toBeNull()
  })

  it("retains trash history when atomic Undo fails", async () => {
    const storage = createNoteStorage()
    const realm = makeMe.aNoteRealm.title("Retry me").please()
    storage.refreshNoteRealm(realm)
    mockSdkService(NoteController, "trashNote", realm)
    await storage.storedApi().trashNote(router, realm.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })
    mockSdkService(NoteController, "undoTrashNote", realm).mockResolvedValue(
      wrapSdkError("destination conflict")
    )

    await expect(storage.storedApi().undo(router)).rejects.toThrow(
      "destination conflict"
    )
    expect(storage.peekUndo()).toMatchObject({
      type: "trash note",
      noteId: realm.id,
    })
  })

  it("keeps the trashed note in cache instead of removing it", async () => {
    const storage = createNoteStorage()
    const realm = makeMe.aNoteRealm.please()
    storage.refreshNoteRealm(realm)
    mockSdkService(NoteController, "trashNote", realm)

    await storage.storedApi().trashNote(router, realm.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })

    expect(storage.refOfNoteRealm(realm.id).value).toBeTruthy()
  })

  it("navigates away before applying the trashed realm, so a still-mounted sidebar cannot react to the note's new trash ancestry", async () => {
    const storage = createNoteStorage()
    const realm = makeMe.aNoteRealm.inFolder(901, "Work").please()
    storage.refreshNoteRealm(realm)
    const trashedRealm = makeMe.aNoteRealm.id(realm.id).please()
    mockSdkService(NoteController, "trashNote", trashedRealm)
    const refreshSpy = vi.spyOn(storage, "refreshNoteRealm")
    refreshSpy.mockClear()

    await storage.storedApi().trashNote(router, realm.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })

    const navigateOrder = routerReplace.mock.invocationCallOrder[0]!
    const refreshOrder = refreshSpy.mock.invocationCallOrder[0]!
    expect(navigateOrder).toBeLessThan(refreshOrder)
  })

  it("refreshes sidebar structural listings after trash", async () => {
    const storage = createNoteStorage()
    const realm = makeMe.aNoteRealm.please()
    storage.refreshNoteRealm(realm)
    mockSdkService(NoteController, "trashNote", realm)
    const before = sidebarStructuralRefreshKey.value

    await storage.storedApi().trashNote(router, realm.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })

    expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
  })
})

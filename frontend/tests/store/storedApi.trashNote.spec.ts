import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { sidebarStructuralRefreshKey } from "@/components/notes/sidebarStructuralRefresh"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import { describe, it, expect, vi, beforeEach } from "vitest"

describe("storedApiCollection trash note", () => {
  const routerReplace = vi.fn()
  const routerPush = vi.fn()
  const router = {
    replace: routerReplace,
    push: routerPush,
  } as unknown as Router

  beforeEach(() => vi.clearAllMocks())

  it.each([
    {
      realm: makeMe.aNoteRealm.title("Root title").please(),
      expectedLocation: (
        realm: ReturnType<typeof makeMe.aNoteRealm.please>
      ) => ({
        name: "notebookPage",
        params: { notebookId: realm.notebookRealm.notebook.id },
      }),
      originalFolderId: null,
    },
    {
      realm: makeMe.aNoteRealm
        .title("Folder title")
        .inFolder(901, "Work")
        .please(),
      expectedLocation: (
        realm: ReturnType<typeof makeMe.aNoteRealm.please>
      ) => ({
        name: "folderPage",
        params: { notebookId: realm.notebookRealm.notebook.id, folderId: 901 },
      }),
      originalFolderId: 901,
    },
  ])(
    "records the original title/placement and keeps $originalFolderId navigation",
    async ({ realm, expectedLocation, originalFolderId }) => {
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
      expect(routerReplace).toHaveBeenCalledWith(expectedLocation(realm))
    }
  )

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

  it("navigates to the source note when reducing to a source property", async () => {
    const storage = createNoteStorage()
    const realm = makeMe.aNoteRealm.please()
    storage.refreshNoteRealm(realm)
    mockSdkService(NoteController, "trashNote", realm)
    const sourceNoteId = 501

    await storage.storedApi().trashNote(router, realm.id, {
      referenceHandling: "REDUCE_TO_SOURCE_PROPERTY",
      sourcePropertyKey: "a part of",
      sourceNoteId,
    })

    expect(routerReplace).toHaveBeenCalledWith(noteShowLocation(sourceNoteId))
  })
})

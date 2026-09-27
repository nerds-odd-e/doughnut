import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { resetNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
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
  })

  afterEach(() => {
    teardownGlobalClientForTesting()
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
      const storage = resetNoteStore()
      storage.refreshNoteRealm(realm)
      const trashedRealm = makeMe.aNoteRealm
        .id(realm.id)
        .title(`${realm.note.noteTopology.title} (2)`)
        .please()
      const trashSpy = mockSdkService(NoteController, "trashNote", trashedRealm)

      await storage.trashNote(realm.id, {
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

  it("undoes trash atomically with the original title/placement and consumes history after success", async () => {
    const storage = resetNoteStore()
    const realm = makeMe.aNoteRealm
      .title("Original title")
      .inFolder(901, "Work")
      .please()
    storage.refreshNoteRealm(realm)
    mockSdkService(NoteController, "trashNote", realm)
    await storage.trashNote(realm.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })
    const undoSpy = mockSdkService(NoteController, "undoTrashNote", realm)

    await storage.undo(router)

    expect(undoSpy).toHaveBeenCalledWith({
      path: { note: realm.id },
      body: { priorTitle: "Original title", priorFolderId: 901 },
    })
    expect(storage.peekUndo()).toBeNull()
  })

  it("retains trash history when atomic Undo fails", async () => {
    const storage = resetNoteStore()
    const realm = makeMe.aNoteRealm.title("Retry me").please()
    storage.refreshNoteRealm(realm)
    mockSdkService(NoteController, "trashNote", realm)
    await storage.trashNote(realm.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })
    mockSdkService(NoteController, "undoTrashNote", realm).mockResolvedValue(
      wrapSdkError("destination conflict")
    )

    await expect(storage.undo(router)).rejects.toThrow("destination conflict")
    expect(storage.peekUndo()).toMatchObject({
      type: "trash note",
      noteId: realm.id,
    })
  })
})

import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { resetNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("noteStore trash note", () => {
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
      const noteStore = resetNoteStore()
      noteStore.refreshNoteRealm(realm)
      const trashedRealm = makeMe.aNoteRealm
        .id(realm.id)
        .title(`${realm.note.noteTopology.title} (2)`)
        .please()
      const trashSpy = mockSdkService(NoteController, "trashNote", trashedRealm)

      await noteStore.trashNote(realm.id, {
        referenceHandling: "REMOVE_FROM_PROPERTIES",
      })

      expect(trashSpy).toHaveBeenCalledWith({
        path: { note: realm.id },
        body: { referenceHandling: "REMOVE_FROM_PROPERTIES" },
      })
      expect(noteStore.peekUndo()).toEqual({
        type: "trash note",
        noteId: realm.id,
        originalTitle: realm.note.noteTopology.title,
        originalFolderId,
      })
    }
  )

  it("undoes trash atomically with the original title/placement and consumes history after success", async () => {
    const noteStore = resetNoteStore()
    const realm = makeMe.aNoteRealm
      .title("Original title")
      .inFolder(901, "Work")
      .please()
    noteStore.refreshNoteRealm(realm)
    mockSdkService(NoteController, "trashNote", realm)
    await noteStore.trashNote(realm.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })
    const undoSpy = mockSdkService(NoteController, "undoTrashNote", realm)

    await noteStore.undo(router)

    expect(undoSpy).toHaveBeenCalledWith({
      path: { note: realm.id },
      body: { priorTitle: "Original title", priorFolderId: 901 },
    })
    expect(noteStore.peekUndo()).toBeNull()
  })

  it("retains trash history when atomic Undo fails", async () => {
    const noteStore = resetNoteStore()
    const realm = makeMe.aNoteRealm.title("Retry me").please()
    noteStore.refreshNoteRealm(realm)
    mockSdkService(NoteController, "trashNote", realm)
    await noteStore.trashNote(realm.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })
    mockSdkService(NoteController, "undoTrashNote", realm).mockResolvedValue(
      wrapSdkError("destination conflict")
    )

    await expect(noteStore.undo(router)).rejects.toThrow("destination conflict")
    expect(noteStore.peekUndo()).toMatchObject({
      type: "trash note",
      noteId: realm.id,
    })
  })
})

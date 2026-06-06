import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { noteShowLocation } from "@/routes/noteShowLocation"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { sidebarStructuralRefreshKey } from "@/components/notes/sidebarStructuralRefresh"
import { describe, it, expect, vi, beforeEach } from "vitest"

describe("storedApiCollection delete note", () => {
  const note = makeMe.aNoteRealm.please()
  const storageAccessor = useStorageAccessor()
  const routerReplace = vi.fn()
  const routerPush = vi.fn()
  const router = {
    replace: routerReplace,
    push: routerPush,
  } as unknown as Router

  beforeEach(() => {
    vi.clearAllMocks()
    storageAccessor.value = createNoteStorage()
  })

  const parentNote = makeMe.aNoteRealm.please()
  let deleteNoteSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    deleteNoteSpy = mockSdkService(NoteController, "deleteNote", [parentNote])
  })

  it("should call the api", async () => {
    const sa = storageAccessor.value.storedApi()
    await sa.deleteNote(router, note.id, {
      referenceHandling: "REMOVE_FROM_PROPERTIES",
    })
    expect(deleteNoteSpy).toHaveBeenCalledTimes(1)
    expect(deleteNoteSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { referenceHandling: "REMOVE_FROM_PROPERTIES" },
    })
    expect(routerReplace).toHaveBeenCalledTimes(1)
    expect(routerReplace).toHaveBeenCalledWith({
      name: "notebookPage",
      params: { notebookId: parentNote.notebookRealm.notebook.id },
    })
  })

  it("should remove the deleted note from cache", async () => {
    storageAccessor.value.refreshNoteRealm(note)
    expect(storageAccessor.value.refOfNoteRealm(note.id).value).toBeTruthy()

    const sa = storageAccessor.value.storedApi()
    await sa.deleteNote(router, note.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })

    expect(storageAccessor.value.refOfNoteRealm(note.id).value).toBeUndefined()
    expect(routerReplace).toHaveBeenCalledWith({
      name: "notebookPage",
      params: { notebookId: note.notebookRealm.notebook.id },
    })
  })

  it("should navigate to notebook when delete returns no realms", async () => {
    mockSdkService(NoteController, "deleteNote", [])
    storageAccessor.value.refreshNoteRealm(note)

    const sa = storageAccessor.value.storedApi()
    await sa.deleteNote(router, note.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })

    expect(routerReplace).toHaveBeenCalledWith({
      name: "notebookPage",
      params: { notebookId: note.notebookRealm.notebook.id },
    })
  })

  it("should navigate to folderPage when the note was inside a folder", async () => {
    const folderId = 901
    const noteInFolder = makeMe.aNoteRealm.inFolder(folderId, "Work").please()
    mockSdkService(NoteController, "deleteNote", [parentNote])
    storageAccessor.value.refreshNoteRealm(noteInFolder)

    const sa = storageAccessor.value.storedApi()
    await sa.deleteNote(router, noteInFolder.id, {
      referenceHandling: "REMOVE_FROM_PROPERTIES",
    })

    expect(routerReplace).toHaveBeenCalledWith({
      name: "folderPage",
      params: {
        notebookId: noteInFolder.notebookRealm.notebook.id,
        folderId,
      },
    })
  })

  it("refreshes sidebar structural listings after deleteNote", async () => {
    storageAccessor.value.refreshNoteRealm(note)
    const before = sidebarStructuralRefreshKey.value
    const sa = storageAccessor.value.storedApi()
    await sa.deleteNote(router, note.id, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })
    expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
  })

  it("should navigate to source note when reduce to source property", async () => {
    const sourceNoteId = 501
    mockSdkService(NoteController, "deleteNote", [])
    storageAccessor.value.refreshNoteRealm(note)

    const sa = storageAccessor.value.storedApi()
    await sa.deleteNote(router, note.id, {
      referenceHandling: "REDUCE_TO_SOURCE_PROPERTY",
      sourcePropertyKey: "a part of",
      sourceNoteId,
    })

    expect(routerReplace).toHaveBeenCalledWith(noteShowLocation(sourceNoteId))
    expect(routerReplace).not.toHaveBeenCalledWith(
      expect.objectContaining({ name: "folderPage" })
    )
    expect(routerReplace).not.toHaveBeenCalledWith(
      expect.objectContaining({ name: "notebookPage" })
    )
  })
})

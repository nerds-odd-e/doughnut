import {
  NoteController,
  RelationController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { noteShowLocation } from "@/routes/noteShowLocation"
import createNoteStorage from "@/store/createNoteStorage"
import NoteEditingHistory from "@/store/NoteEditingHistory"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { sidebarStructuralRefreshKey } from "@/components/notes/sidebarStructuralRefresh"
import { describe, it, expect, vi, beforeEach } from "vitest"

describe("storedApiCollection", () => {
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

  describe("undo create note", () => {
    const parentNote = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService(NoteController, "trashNote", parentNote)
    })

    it("should remove the created note from cache after undo", async () => {
      const noteEditingHistory = new NoteEditingHistory()
      storageAccessor.value = createNoteStorage(noteEditingHistory)

      storageAccessor.value.refreshNoteRealm(note)
      noteEditingHistory.createNote(note.id)

      expect(storageAccessor.value.refOfNoteRealm(note.id).value).toBeTruthy()

      const sa = storageAccessor.value.storedApi()
      await sa.undo(router)

      expect(
        storageAccessor.value.refOfNoteRealm(note.id).value
      ).toBeUndefined()
    })

    it("should navigate to notebook page when trash returns no realms", async () => {
      mockSdkService(NoteController, "trashNote", note)
      const noteEditingHistory = new NoteEditingHistory()
      storageAccessor.value = createNoteStorage(noteEditingHistory)

      storageAccessor.value.refreshNoteRealm(note)
      noteEditingHistory.createNote(note.id)

      const sa = storageAccessor.value.storedApi()
      await sa.undo(router)

      expect(routerPush).toHaveBeenCalledWith({
        name: "notebookPage",
        params: { notebookId: note.notebookRealm.notebook.id },
      })
    })

    it("uses the recoverable trash path with LEAVE_DEAD_LINKS when undoing note creation", async () => {
      const trashSpy = mockSdkService(NoteController, "trashNote", note)
      const noteEditingHistory = new NoteEditingHistory()
      storageAccessor.value = createNoteStorage(noteEditingHistory)

      storageAccessor.value.refreshNoteRealm(note)
      noteEditingHistory.createNote(note.id)

      const sa = storageAccessor.value.storedApi()
      await sa.undo(router)

      expect(trashSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { referenceHandling: "LEAVE_DEAD_LINKS" },
      })
    })
  })

  describe("completeContent", () => {
    let updateNoteContentSpy: ReturnType<typeof mockSdkService>
    let showNoteSpy: ReturnType<typeof mockSdkService>
    let noteRef

    beforeEach(() => {
      vi.clearAllMocks()
      updateNoteContentSpy = mockSdkService(
        TextContentController,
        "updateNoteContent",
        note
      )
      showNoteSpy = mockSdkService(NoteController, "showNote", note)
      noteRef = storageAccessor.value.refOfNoteRealm(note.id)
    })

    it("does nothing when no completion value is provided", async () => {
      const sa = storageAccessor.value.storedApi()
      await sa.completeContent(note.id)
      expect(updateNoteContentSpy).not.toHaveBeenCalled()
    })

    it("updates note content with completion", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { content: "Hello " } }

      await sa.completeContent(note.id, {
        content: "Hello world!",
      })

      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          content: "Hello world!",
        },
      })
    })

    it("loads note first if not in storage", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = undefined

      await sa.completeContent(note.id, {
        content: "<p>Desc</p>world!",
      })

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: note.id },
      })
      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          content: "<p>Desc</p>world!",
        },
      })
    })
  })

  describe("move note", () => {
    it("refreshes sidebar structural listings after moveNoteToFolder", async () => {
      mockSdkService(RelationController, "moveNoteToFolder", [note])
      storageAccessor.value.refreshNoteRealm(note)
      const before = sidebarStructuralRefreshKey.value
      const sa = storageAccessor.value.storedApi()
      await sa.moveNoteToFolder(note.id, 99)
      expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
    })

    it("refreshes sidebar structural listings after moveNoteToNotebookRoot", async () => {
      mockSdkService(RelationController, "moveNoteToNotebookRootInNotebook", [
        note,
      ])
      storageAccessor.value.refreshNoteRealm(note)
      const before = sidebarStructuralRefreshKey.value
      const sa = storageAccessor.value.storedApi()
      await sa.moveNoteToNotebookRoot(note.id, note.notebookRealm.notebook.id)
      expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
    })
  })

  describe("upload note image", () => {
    const file = new File(["png"], "Blue.PNG", { type: "image/png" })

    it("refreshes sidebar structural listings after an accepted upload", async () => {
      mockSdkService(NoteController, "uploadNoteImage", note)
      const before = sidebarStructuralRefreshKey.value
      const sa = storageAccessor.value.storedApi()
      await sa.uploadNoteImage(note.id, file)
      expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
    })

    it("leaves sidebar structural listings alone after a refused upload", async () => {
      vi.spyOn(NoteController, "uploadNoteImage").mockResolvedValue(
        wrapSdkError("refused")
      )
      const before = sidebarStructuralRefreshKey.value
      const sa = storageAccessor.value.storedApi()
      await sa.uploadNoteImage(note.id, file)
      expect(sidebarStructuralRefreshKey.value).toBe(before)
    })
  })

  describe("focusNoteRealm", () => {
    it("refreshes cache, sidebar listings, and navigates to the note", async () => {
      const sa = storageAccessor.value.storedApi()
      const before = sidebarStructuralRefreshKey.value

      await sa.focusNoteRealm(router, note)

      expect(storageAccessor.value.refOfNoteRealm(note.id).value).toBeTruthy()
      expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
      expect(routerReplace).toHaveBeenCalledWith(noteShowLocation(note.id))
    })
  })

  describe("refreshWikiLinkCacheForNote", () => {
    let updateNoteContentSpy: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      vi.clearAllMocks()
      updateNoteContentSpy = mockSdkService(
        TextContentController,
        "updateNoteContent",
        note
      )
    })

    it("calls updateNoteContent even when content matches stored note", async () => {
      const sameBody = "same body"
      const sa = storageAccessor.value.storedApi()
      storageAccessor.value.refreshNoteRealm({
        ...note,
        note: { ...note.note, content: sameBody },
      })

      await sa.refreshWikiLinkCacheForNote(note.id)

      expect(updateNoteContentSpy).toHaveBeenCalledTimes(1)
      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { content: sameBody },
      })
    })
  })
})

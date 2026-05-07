import type { Router } from "vue-router"
import createNoteStorage from "@/store/createNoteStorage"
import NoteEditingHistory from "@/store/NoteEditingHistory"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
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

  describe("delete note", () => {
    const parentNote = makeMe.aNoteRealm.please()
    let deleteNoteSpy: ReturnType<typeof mockSdkService<"deleteNote">>

    beforeEach(() => {
      deleteNoteSpy = mockSdkService("deleteNote", [parentNote])
    })

    it("should call the api", async () => {
      const sa = storageAccessor.value.storedApi()
      await sa.deleteNote(router, note.id)
      expect(deleteNoteSpy).toHaveBeenCalledTimes(1)
      expect(deleteNoteSpy).toHaveBeenCalledWith({ path: { note: note.id } })
      expect(routerReplace).toHaveBeenCalledTimes(1)
      expect(routerReplace).toHaveBeenCalledWith({
        name: "notebookPage",
        params: { notebookId: parentNote.notebookView.notebook.id },
      })
    })

    it("should remove the deleted note from cache", async () => {
      storageAccessor.value.refreshNoteRealm(note)
      expect(storageAccessor.value.refOfNoteRealm(note.id).value).toBeTruthy()

      const sa = storageAccessor.value.storedApi()
      await sa.deleteNote(router, note.id)

      expect(
        storageAccessor.value.refOfNoteRealm(note.id).value
      ).toBeUndefined()
      expect(routerReplace).toHaveBeenCalledWith({
        name: "notebookPage",
        params: { notebookId: note.notebookView.notebook.id },
      })
    })

    it("should navigate to notebook when delete returns no realms", async () => {
      mockSdkService("deleteNote", [])
      storageAccessor.value.refreshNoteRealm(note)

      const sa = storageAccessor.value.storedApi()
      await sa.deleteNote(router, note.id)

      expect(routerReplace).toHaveBeenCalledWith({
        name: "notebookPage",
        params: { notebookId: note.notebookView.notebook.id },
      })
    })
  })

  describe("undo create note", () => {
    const parentNote = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService("deleteNote", [parentNote])
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

    it("should navigate to notebook page when delete returns no realms", async () => {
      mockSdkService("deleteNote", [])
      const noteEditingHistory = new NoteEditingHistory()
      storageAccessor.value = createNoteStorage(noteEditingHistory)

      storageAccessor.value.refreshNoteRealm(note)
      noteEditingHistory.createNote(note.id)

      const sa = storageAccessor.value.storedApi()
      await sa.undo(router)

      expect(routerPush).toHaveBeenCalledWith({
        name: "notebookPage",
        params: { notebookId: note.notebookView.notebook.id },
      })
    })
  })

  describe("completeContent", () => {
    let updateNoteContentSpy: ReturnType<
      typeof mockSdkService<"updateNoteContent">
    >
    let showNoteSpy: ReturnType<typeof mockSdkService<"showNote">>
    let noteRef

    beforeEach(() => {
      vi.clearAllMocks()
      updateNoteContentSpy = mockSdkService("updateNoteContent", note)
      showNoteSpy = mockSdkService("showNote", note)
      noteRef = storageAccessor.value.refOfNoteRealm(note.id)
    })

    it("should do nothing when no completion value is provided", async () => {
      const sa = storageAccessor.value.storedApi()
      await sa.completeContent(note.id)
      expect(updateNoteContentSpy).not.toHaveBeenCalled()
    })

    it("should update note details with completion", async () => {
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

    it("should replace entire note details with completion", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { content: "Hello world" } }

      await sa.completeContent(note.id, {
        content: "Hello !",
      })

      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          content: "Hello !",
        },
      })
    })

    it("should load note first if not in storage", async () => {
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

  describe("refreshWikiLinkCacheForNote", () => {
    let updateNoteContentSpy: ReturnType<
      typeof mockSdkService<"updateNoteContent">
    >

    beforeEach(() => {
      vi.clearAllMocks()
      updateNoteContentSpy = mockSdkService("updateNoteContent", note)
    })

    it("calls updateNoteContent even when details match stored note", async () => {
      const detailsBody = "same details"
      const sa = storageAccessor.value.storedApi()
      storageAccessor.value.refreshNoteRealm({
        ...note,
        note: { ...note.note, content: detailsBody },
      })

      await sa.refreshWikiLinkCacheForNote(note.id)

      expect(updateNoteContentSpy).toHaveBeenCalledTimes(1)
      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { content: detailsBody },
      })
    })
  })
})

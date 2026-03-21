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
    })

    it("should remove the deleted note from cache", async () => {
      storageAccessor.value.refreshNoteRealm(note)
      expect(storageAccessor.value.refOfNoteRealm(note.id).value).toBeTruthy()

      const sa = storageAccessor.value.storedApi()
      await sa.deleteNote(router, note.id)

      expect(
        storageAccessor.value.refOfNoteRealm(note.id).value
      ).toBeUndefined()
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
  })

  describe("completeDetails", () => {
    let updateNoteDetailsSpy: ReturnType<
      typeof mockSdkService<"updateNoteDetails">
    >
    let showNoteSpy: ReturnType<typeof mockSdkService<"showNote">>
    let noteRef

    beforeEach(() => {
      vi.clearAllMocks()
      updateNoteDetailsSpy = mockSdkService("updateNoteDetails", note)
      showNoteSpy = mockSdkService("showNote", note)
      noteRef = storageAccessor.value.refOfNoteRealm(note.id)
    })

    it("should do nothing when no completion value is provided", async () => {
      const sa = storageAccessor.value.storedApi()
      await sa.completeDetails(note.id)
      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()
    })

    it("should update note details with completion", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "Hello " } }

      await sa.completeDetails(note.id, {
        details: "Hello world!",
      })

      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: "Hello world!",
        },
      })
    })

    it("should replace entire note details with completion", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "Hello world" } }

      await sa.completeDetails(note.id, {
        details: "Hello !",
      })

      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: "Hello !",
        },
      })
    })

    it("should load note first if not in storage", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = undefined

      await sa.completeDetails(note.id, {
        details: "<p>Desc</p>world!",
      })

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: note.id },
      })
      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: "<p>Desc</p>world!",
        },
      })
    })
  })
})

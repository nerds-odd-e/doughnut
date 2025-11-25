import type { Router } from "vue-router"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "@tests/fixtures/makeMe"
import { mockSdkServiceWithImplementation } from "@tests/helpers"

describe("storedApiCollection", () => {
  const note = makeMe.aNoteRealm.please()
  const storageAccessor = createNoteStorage()
  const routerReplace = vitest.fn()
  const router = { replace: routerReplace } as unknown as Router
  const sa = storageAccessor.storedApi()

  describe("delete note", () => {
    let deleteNote

    beforeEach(() => {
      deleteNote = vi.fn().mockResolvedValue([note])
      mockSdkServiceWithImplementation("deleteNote", async (options) => {
        return await deleteNote(options)
      })
    })

    it("should call the api", async () => {
      await sa.deleteNote(router, note.id)
      expect(deleteNote).toHaveBeenCalledTimes(1)
      expect(deleteNote).toHaveBeenCalledWith({ path: { note: note.id } })
      expect(routerReplace).toHaveBeenCalledTimes(1)
    })
  })

  describe("completeDetails", () => {
    let updateNoteDetails
    let showNote
    let noteRef

    beforeEach(() => {
      updateNoteDetails = vi.fn().mockResolvedValue(note)
      showNote = vi.fn().mockResolvedValue(note)
      mockSdkServiceWithImplementation("updateNoteDetails", async (options) => {
        return await updateNoteDetails(options)
      })
      mockSdkServiceWithImplementation("showNote", async (options) => {
        return await showNote(options)
      })
      noteRef = storageAccessor.refOfNoteRealm(note.id)
    })

    it("should do nothing when no completion value is provided", async () => {
      await sa.completeDetails(note.id)
      expect(updateNoteDetails).not.toHaveBeenCalled()
    })

    it("should update note details with completion", async () => {
      noteRef.value = { ...note, note: { details: "Hello " } }

      await sa.completeDetails(note.id, {
        completion: "world!",
        deleteFromEnd: 0,
      })

      expect(updateNoteDetails).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: "Hello world!",
        },
      })
    })

    it("should delete characters before adding completion", async () => {
      noteRef.value = { ...note, note: { details: "Hello world" } }

      await sa.completeDetails(note.id, {
        completion: "!",
        deleteFromEnd: 5,
      })

      expect(updateNoteDetails).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: "Hello !",
        },
      })
    })

    it("should load note first if not in storage", async () => {
      noteRef.value = undefined

      await sa.completeDetails(note.id, {
        completion: "world!",
        deleteFromEnd: 0,
      })

      expect(showNote).toHaveBeenCalledWith({ path: { note: note.id } })
      expect(updateNoteDetails).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: "<p>Desc</p>world!",
        },
      })
    })
  })
})

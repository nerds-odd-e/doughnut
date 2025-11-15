import type { Router } from "vue-router"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("storedApiCollection", () => {
  const note = makeMe.aNoteRealm.please()
  const managedApi = helper.managedApi
  const storageAccessor = createNoteStorage(managedApi)
  const routerReplace = vitest.fn()
  const router = { replace: routerReplace } as unknown as Router
  const sa = storageAccessor.storedApi()

  describe("delete note", () => {
    let deleteNote

    beforeEach(() => {
      deleteNote = vi.fn().mockResolvedValue(note)
      managedApi.restNoteController.deleteNote = deleteNote
    })

    it("should call the api", async () => {
      await sa.deleteNote(router, note.id)
      expect(deleteNote).toHaveBeenCalledTimes(1)
      expect(deleteNote).toHaveBeenCalledWith(note.id)
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
      managedApi.restTextContentController.updateNoteDetails = updateNoteDetails
      managedApi.restNoteController.show = showNote
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

      expect(updateNoteDetails).toHaveBeenCalledWith(note.id, {
        details: "Hello world!",
      })
    })

    it("should delete characters before adding completion", async () => {
      noteRef.value = { ...note, note: { details: "Hello world" } }

      await sa.completeDetails(note.id, {
        completion: "!",
        deleteFromEnd: 5,
      })

      expect(updateNoteDetails).toHaveBeenCalledWith(note.id, {
        details: "Hello !",
      })
    })

    it("should load note first if not in storage", async () => {
      noteRef.value = undefined

      await sa.completeDetails(note.id, {
        completion: "world!",
        deleteFromEnd: 0,
      })

      expect(showNote).toHaveBeenCalledWith(note.id)
      expect(updateNoteDetails).toHaveBeenCalledWith(note.id, {
        details: "<p>Desc</p>world!",
      })
    })
  })
})

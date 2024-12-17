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
})

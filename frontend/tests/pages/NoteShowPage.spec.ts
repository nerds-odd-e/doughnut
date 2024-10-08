import NoteShowPage from "@/pages/NoteShowPage.vue"
import { screen } from "@testing-library/vue"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

describe("all in note show page", () => {
  describe("note show", () => {
    const noteRealm = makeMe.aNoteRealm.inCircle("a circle").please()

    beforeEach(() => {
      helper.managedApi.restNoteController.show1 = vi
        .fn()
        .mockResolvedValue(noteRealm)
    })

    it(" should fetch API", async () => {
      helper
        .component(NoteShowPage)
        .withStorageProps({ noteId: noteRealm.id })
        .render()
      await screen.findByText(noteRealm.note.noteTopic.topicConstructor)
      expect(helper.managedApi.restNoteController.show1).toBeCalledWith(
        noteRealm.id
      )
    })
  })
})

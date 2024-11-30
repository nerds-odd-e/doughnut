import NoteShowPage from "@/pages/NoteShowPage.vue"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("all in note show page", () => {
  describe("note show", () => {
    const noteRealm = makeMe.aNoteRealm.inCircle("a circle").please()

    beforeEach(() => {
      helper.managedApi.restNoteController.show = vi
        .fn()
        .mockResolvedValue(noteRealm)
    })

    it(" should fetch API", async () => {
      helper
        .component(NoteShowPage)
        .withStorageProps({ noteId: noteRealm.id })
        .render()
      await screen.findByText(noteRealm.note.noteTopic.topicConstructor)
      expect(helper.managedApi.restNoteController.show).toBeCalledWith(
        noteRealm.id
      )
    })
  })
})

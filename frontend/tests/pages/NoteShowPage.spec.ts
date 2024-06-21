import NoteShowPage from "@/pages/NoteShowPage.vue"
import { screen } from "@testing-library/vue"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

describe("all in note show page", () => {
  beforeEach(() => {
    const teleportTarget = document.createElement("div")
    teleportTarget.id = "head-status"
    document.body.appendChild(teleportTarget)
  })

  describe("note show", () => {
    const noteRealm = makeMe.aNoteRealm.inCircle("a circle").please()

    it(" should fetch API", async () => {
      helper.managedApi.restNoteController.show1 = vi
        .fn()
        .mockResolvedValue(noteRealm)
      helper
        .component(NoteShowPage)
        .withStorageProps({ noteId: noteRealm.id })
        .render()
      await screen.findByText(noteRealm.note.noteTopic.topicConstructor)
      expect(helper.managedApi.restNoteController.show1).toBeCalledWith(
        noteRealm.id,
      )
    })
  })
})

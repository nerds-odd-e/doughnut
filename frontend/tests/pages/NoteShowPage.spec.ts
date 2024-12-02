import NoteShowPage from "@/pages/NoteShowPage.vue"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

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

  describe("conversation maximize/minimize", () => {
    it("should maximize conversation when maximize button is clicked", async () => {
      const note = makeMe.aNoteRealm.please()
      helper.managedApi.restNoteController.show = vitest
        .fn()
        .mockResolvedValue(note)
      helper.managedApi.restConversationMessageController.getConversationsAboutNote =
        vitest.fn().mockResolvedValue([])

      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withStorageProps({ noteId: note.id })
        .mount()

      await flushPromises()

      // Show conversation
      await wrapper
        .find('[title="Star a conversation about this note"]')
        .trigger("click")

      // Click maximize button
      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      // Click restore button
      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
    })

    it("should restore maximized state before closing conversation", async () => {
      const note = makeMe.aNoteRealm.please()
      helper.managedApi.restNoteController.show = vitest
        .fn()
        .mockResolvedValue(note)
      helper.managedApi.restConversationMessageController.getConversationsAboutNote =
        vitest.fn().mockResolvedValue([])

      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withStorageProps({ noteId: note.id })
        .mount()

      await flushPromises()

      // Show conversation
      await wrapper
        .find('[title="Star a conversation about this note"]')
        .trigger("click")

      // Maximize conversation
      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      // Close conversation while maximized
      await wrapper.find('[aria-label="Close dialog"]').trigger("click")

      // Verify note content is visible again
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
      // Verify conversation is closed
      expect(wrapper.find(".conversation-container").exists()).toBe(false)
    })
  })
})

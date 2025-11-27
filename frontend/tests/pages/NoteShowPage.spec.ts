import NoteShowPage from "@/pages/NoteShowPage.vue"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockShowNoteAccessory, mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

describe("all in note show page", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
    mockShowNoteAccessory()
  })

  describe("note show", () => {
    const noteRealm = makeMe.aNoteRealm.inCircle("a circle").please()

    beforeEach(() => {
      mockSdkService("showNote", noteRealm)
    })

    it(" should fetch API", async () => {
      const showNoteSpy = mockSdkService("showNote", noteRealm)
      helper
        .component(NoteShowPage)
        .withStorageProps({ noteId: noteRealm.id })
        .withRouter(router)
        .render()
      await screen.findByText(noteRealm.note.noteTopology.titleOrPredicate)
      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteRealm.id },
        client: expect.anything(),
      })
    })
  })

  describe("conversation maximize/minimize", () => {
    it("should maximize conversation when maximize button is clicked", async () => {
      const note = makeMe.aNoteRealm.please()
      mockSdkService("showNote", note)
      mockSdkService("getConversationsAboutNote", [])

      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withStorageProps({ noteId: note.id })
        .withRouter(router)
        .mount()

      await flushPromises()

      // Show conversation by updating URL
      await router.push({
        name: "noteShow",
        params: { noteId: note.id },
        query: { conversation: "true" },
      })
      await flushPromises()

      // Click maximize button
      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      // Click restore button
      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
    })

    it("should restore maximized state before closing conversation", async () => {
      const note = makeMe.aNoteRealm.please()
      mockSdkService("showNote", note)
      mockSdkService("getConversationsAboutNote", [])

      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withStorageProps({ noteId: note.id })
        .withRouter(router)
        .mount()

      await flushPromises()

      // Show conversation by updating URL
      await router.push({
        name: "noteShow",
        params: { noteId: note.id },
        query: { conversation: "true" },
      })
      await flushPromises()

      // Maximize conversation
      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      // Close conversation while maximized
      await wrapper.find('[aria-label="Close dialog"]').trigger("click")
      await flushPromises()

      // Verify URL is updated
      expect(router.currentRoute.value.query.conversation).toBeUndefined()

      // Verify note content is visible again
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
      // Verify conversation is closed
      expect(wrapper.find(".conversation-container").exists()).toBe(false)
    })

    it("should open conversation when URL has conversation=true", async () => {
      const note = makeMe.aNoteRealm.please()
      mockSdkService("showNote", note)
      mockSdkService("getConversationsAboutNote", [])

      // Start with conversation in URL
      router.push({
        name: "noteShow",
        params: { noteId: note.id },
        query: { conversation: "true" },
      })
      await flushPromises()

      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withStorageProps({ noteId: note.id })
        .withRouter(router)
        .mount()

      await flushPromises()

      // Verify conversation is open
      expect(wrapper.find(".conversation-wrapper").exists()).toBe(true)
    })
  })
})

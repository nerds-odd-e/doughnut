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
        .withCleanStorage()
        .withProps({ noteId: noteRealm.id })
        .withRouter(router)
        .render()

      await screen.findByText(noteRealm.note.noteTopology.title)

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteRealm.id },
      })
    })
  })

  describe("conversation maximize/minimize", () => {
    const note = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService("showNote", note)
      mockSdkService("getConversationsAboutNote", [])
    })

    it("should maximize conversation when maximize button is clicked", async () => {
      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({ noteId: note.id })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: { noteId: note.id },
        query: { conversation: "true" },
      })
      await flushPromises()

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
    })

    it("should restore maximized state before closing conversation", async () => {
      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({ noteId: note.id })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: { noteId: note.id },
        query: { conversation: "true" },
      })
      await flushPromises()

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      await wrapper.find('[aria-label="Close dialog"]').trigger("click")
      await flushPromises()

      expect(router.currentRoute.value.query.conversation).toBeUndefined()
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
      expect(wrapper.find(".conversation-container").exists()).toBe(false)
    })

    it("should open conversation when URL has conversation=true", async () => {
      router.push({
        name: "noteShow",
        params: { noteId: note.id },
        query: { conversation: "true" },
      })
      await flushPromises()

      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({ noteId: note.id })
        .withRouter(router)
        .mount()

      await flushPromises()

      expect(wrapper.find(".conversation-wrapper").exists()).toBe(true)
    })
  })
})

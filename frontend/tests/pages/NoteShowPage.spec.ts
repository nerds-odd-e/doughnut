import NoteShowPage from "@/pages/NoteShowPage.vue"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import * as sdk from "@generated/backend/sdk.gen"

describe("all in note show page", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
    vi.spyOn(sdk, "showNoteAccessory").mockResolvedValue({
      data: undefined as never,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
  })

  describe("note show", () => {
    const noteRealm = makeMe.aNoteRealm.inCircle("a circle").please()

    beforeEach(() => {
      vi.spyOn(helper.managedApi.services, "showNote").mockResolvedValue(
        noteRealm as never
      )
    })

    it(" should fetch API", async () => {
      helper
        .component(NoteShowPage)
        .withStorageProps({ noteId: noteRealm.id })
        .withRouter(router)
        .render()
      await screen.findByText(noteRealm.note.noteTopology.titleOrPredicate)
      expect(helper.managedApi.services.showNote).toBeCalledWith({
        path: { note: noteRealm.id },
      })
    })
  })

  describe("conversation maximize/minimize", () => {
    it("should maximize conversation when maximize button is clicked", async () => {
      const note = makeMe.aNoteRealm.please()
      vi.spyOn(helper.managedApi.services, "showNote").mockResolvedValue(
        note as never
      )
      vi.spyOn(
        helper.managedApi.services,
        "getConversationsAboutNote"
      ).mockResolvedValue([])

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
      vi.spyOn(helper.managedApi.services, "showNote").mockResolvedValue(
        note as never
      )
      vi.spyOn(
        helper.managedApi.services,
        "getConversationsAboutNote"
      ).mockResolvedValue([])

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
      vi.spyOn(helper.managedApi.services, "showNote").mockResolvedValue(
        note as never
      )
      vi.spyOn(
        helper.managedApi.services,
        "getConversationsAboutNote"
      ).mockResolvedValue([])

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

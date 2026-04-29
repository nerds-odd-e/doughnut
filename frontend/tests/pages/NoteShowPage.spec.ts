import NoteShowPage from "@/pages/NoteShowPage.vue"
import { screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockShowNoteAccessory,
  mockSdkService,
  wrapSdkError,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { describe, it, beforeEach, expect, vi } from "vitest"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"

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

      await screen.findByText(noteRealm.note.noteTopology.title!)

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteRealm.id },
      })
    })
  })

  describe("note show by basename", () => {
    const noteRealm = makeMe.aNoteRealm.inCircle("a circle").please()

    beforeEach(() => {
      mockSdkService("showNoteByAmbiguousBasename", noteRealm)
      mockSdkService("showNote", noteRealm)
    })

    it("resolves basename then loads note and passes stable id to NoteShow via storage", async () => {
      const basenameSpy = mockSdkService(
        "showNoteByAmbiguousBasename",
        noteRealm
      )
      const showNoteSpy = mockSdkService("showNote", noteRealm)

      helper
        .component(NoteShowPage)
        .withCleanStorage()
        .withProps({ basename: "my-note" })
        .withRouter(router)
        .render()

      await flushPromises()
      await screen.findByText(noteRealm.note.noteTopology.title!)

      expect(basenameSpy).toHaveBeenCalledWith({
        path: { basename: "my-note" },
      })
      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteRealm.id },
      })
    })

    it("shows error when basename lookup fails", async () => {
      vi.spyOn(NoteController, "showNoteByAmbiguousBasename").mockResolvedValue(
        wrapSdkError({
          message: "More than one note matches this name.",
        }) as never
      )

      helper
        .component(NoteShowPage)
        .withCleanStorage()
        .withProps({ basename: "ambiguous" })
        .withRouter(router)
        .render()

      expect(
        await screen.findByText(/More than one note matches this name/)
      ).toBeTruthy()
    })
  })

  describe("note show by notebook slug path", () => {
    const noteRealm = makeMe.aNoteRealm.inCircle("a circle").please()

    beforeEach(() => {
      mockSdkService("getNoteBySlug", noteRealm)
      mockSdkService("showNote", noteRealm)
    })

    it("resolves slug path then loads note and passes stable id to NoteShow via storage", async () => {
      const slugSpy = mockSdkService("getNoteBySlug", noteRealm)
      const showNoteSpy = mockSdkService("showNote", noteRealm)

      helper
        .component(NoteShowPage)
        .withCleanStorage()
        .withProps({
          notebookId: 99,
          noteSlugPath: "outer/inner/leaf",
        })
        .withRouter(router)
        .render()

      await flushPromises()
      await screen.findByText(noteRealm.note.noteTopology.title!)

      expect(slugSpy).toHaveBeenCalledWith({
        path: { notebook: 99 },
        query: { slugPath: "outer/inner/leaf" },
      })
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

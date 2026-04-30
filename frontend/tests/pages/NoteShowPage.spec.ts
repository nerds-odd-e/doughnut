import NoteShowPage from "@/pages/NoteShowPage.vue"
import { screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockNotebookGetForNoteRealm,
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

  describe("note show by ambiguous slug", () => {
    const noteRealm = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService("showNoteByAmbiguousBasename", noteRealm)
      mockSdkService("showNote", noteRealm)
      mockNotebookGetForNoteRealm(noteRealm, {
        id: 101,
        name: "a circle",
      })
    })

    it("resolves ambiguous slug then loads note and passes stable id to NoteShow via storage", async () => {
      const basenameSpy = mockSdkService(
        "showNoteByAmbiguousBasename",
        noteRealm
      )
      const showNoteSpy = mockSdkService("showNote", noteRealm)

      helper
        .component(NoteShowPage)
        .withCleanStorage()
        .withProps({ slug: "my-note" })
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

    it("shows error when ambiguous slug lookup fails", async () => {
      vi.spyOn(NoteController, "showNoteByAmbiguousBasename").mockResolvedValue(
        wrapSdkError({
          message: "More than one note matches this name.",
        }) as never
      )

      helper
        .component(NoteShowPage)
        .withCleanStorage()
        .withProps({ slug: "ambiguous" })
        .withRouter(router)
        .render()

      expect(
        await screen.findByText(/More than one note matches this name/)
      ).toBeTruthy()
    })
  })

  describe("note show by notebook slug path", () => {
    const noteRealm = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService("getNoteBySlug", noteRealm)
      mockSdkService("showNote", noteRealm)
      mockNotebookGetForNoteRealm(noteRealm, {
        id: 101,
        name: "a circle",
      })
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
      mockSdkService("getNoteBySlug", note)
      mockSdkService("showNote", note)
      mockSdkService("getConversationsAboutNote", [])
      mockNotebookGetForNoteRealm(note)
    })

    it("should maximize conversation when maximize button is clicked", async () => {
      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          notebookId: note.notebookId,
          noteSlugPath: note.slug,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: {
          notebookId: String(note.notebookId),
          noteSlugPath: note.slug,
        },
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
        .withProps({
          notebookId: note.notebookId,
          noteSlugPath: note.slug,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: {
          notebookId: String(note.notebookId),
          noteSlugPath: note.slug,
        },
        query: { conversation: "true" },
      })
      await flushPromises()

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      await wrapper.find('[aria-label="Close dialog"]').trigger("click")
      await flushPromises()

      expect(router.currentRoute.value.name).toBe("noteShow")
      expect(router.currentRoute.value.params.notebookId).toBe(
        String(note.notebookId)
      )
      expect(router.currentRoute.value.params.noteSlugPath).toBe(note.slug)
      expect(router.currentRoute.value.query.conversation).toBeUndefined()
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
      expect(wrapper.find(".conversation-container").exists()).toBe(false)
    })

    it("should open conversation when URL has conversation=true", async () => {
      router.push({
        name: "noteShow",
        params: {
          notebookId: String(note.notebookId),
          noteSlugPath: note.slug,
        },
        query: { conversation: "true" },
      })
      await flushPromises()

      const wrapper = helper
        .component(NoteShowPage)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          notebookId: note.notebookId,
          noteSlugPath: note.slug,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      expect(wrapper.find(".conversation-wrapper").exists()).toBe(true)
    })
  })
})

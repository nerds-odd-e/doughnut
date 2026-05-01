import NoteShowPageWithNotebookSidebarLayout from "@tests/fixtures/NoteShowPageWithNotebookSidebarLayout.vue"
import { screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockNotebookGetForNoteRealm,
  mockShowNoteAccessory,
  mockSdkService,
} from "@tests/helpers"
import { resetNotebookSidebarState } from "@/composables/useCurrentNoteSidebarState"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { describe, it, beforeEach, expect } from "vitest"

describe("all in note show page", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    resetNotebookSidebarState()
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
    mockShowNoteAccessory()
  })

  describe("note show by note id", () => {
    const noteRealm = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService("showNote", noteRealm)
      mockNotebookGetForNoteRealm(noteRealm, {
        id: 101,
        name: "a circle",
      })
    })

    it("loads note by id from route", async () => {
      const showNoteSpy = mockSdkService("showNote", noteRealm)

      helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCleanStorage()
        .withProps({ noteId: noteRealm.id })
        .withRouter(router)
        .render()

      await flushPromises()
      await screen.findByText(noteRealm.note.noteTopology.title!)

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
      mockNotebookGetForNoteRealm(note)
    })

    it("should maximize conversation when maximize button is clicked", async () => {
      const wrapper = helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          noteId: note.id,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: {
          noteId: String(note.id),
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
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          noteId: note.id,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      await router.push({
        name: "noteShow",
        params: {
          noteId: String(note.id),
        },
        query: { conversation: "true" },
      })
      await flushPromises()

      await wrapper.find('[aria-label="Toggle maximize"]').trigger("click")
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(false)

      await wrapper.find('[aria-label="Close dialog"]').trigger("click")
      await flushPromises()

      expect(router.currentRoute.value.name).toBe("noteShow")
      expect(router.currentRoute.value.params.noteId).toBe(String(note.id))
      expect(router.currentRoute.value.query.conversation).toBeUndefined()
      expect(wrapper.find(".note-content-wrapper").exists()).toBe(true)
      expect(wrapper.find(".conversation-container").exists()).toBe(false)
    })

    it("should open conversation when URL has conversation=true", async () => {
      router.push({
        name: "noteShow",
        params: {
          noteId: String(note.id),
        },
        query: { conversation: "true" },
      })
      await flushPromises()

      const wrapper = helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({
          noteId: note.id,
        })
        .withRouter(router)
        .mount()

      await flushPromises()

      expect(wrapper.find(".conversation-wrapper").exists()).toBe(true)
    })
  })
})

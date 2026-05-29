import {
  AiController,
  AssimilationController,
  ConversationMessageController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import {
  resetAssimilationViewForTests,
  useAssimilationView,
} from "@/composables/useAssimilationView"
import NoteShowPageWithNotebookSidebarLayout from "@tests/fixtures/NoteShowPageWithNotebookSidebarLayout.vue"
import { within } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockNotebookGetForNoteRealm,
  mockSdkService,
} from "@tests/helpers"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { describe, it, beforeEach, expect, vi } from "vitest"

describe("all in note show page", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
  })

  describe("note show by note id", () => {
    const noteRealm = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService(NoteController, "showNote", noteRealm)
      mockNotebookGetForNoteRealm(noteRealm, {
        id: 101,
        name: "a circle",
      })
    })

    it("loads note by id from route", async () => {
      const showNoteSpy = mockSdkService(NoteController, "showNote", noteRealm)

      helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCleanStorage()
        .withProps({ noteId: noteRealm.id })
        .withRouter(router)
        .currentRoute(noteShowLocation(noteRealm.id))
        .render()

      await flushPromises()
      await vi.waitFor(() => {
        const main = document.getElementById("main-note-content")
        expect(main).not.toBeNull()
        expect(
          within(main as HTMLElement).getByText(
            noteRealm.note.noteTopology.title
          )
        ).toBeInTheDocument()
      })

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteRealm.id },
      })
    })
  })

  describe("conversation maximize/minimize", () => {
    const note = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService(NoteController, "showNote", note)
      mockSdkService(
        ConversationMessageController,
        "getConversationsAboutNote",
        []
      )
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

  describe("inline assimilation panel", () => {
    const noteRealm = makeMe.aNoteRealm.please()

    beforeEach(() => {
      resetAssimilationViewForTests()
      mockSdkService(NoteController, "showNote", noteRealm)
      mockNotebookGetForNoteRealm(noteRealm, {
        id: 101,
        name: "a circle",
      })
      mockSdkService(NoteController, "getNoteInfo", {})
      mockSdkService(AiController, "generateUnderstandingChecklist", {
        points: [],
      })
      mockSdkService(AssimilationController, "assimilate", [])
    })

    it("renders keep-for-recall when assimilation settings are on", async () => {
      useAssimilationView().requestOnFor(noteRealm.id)

      const wrapper = helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({ noteId: noteRealm.id })
        .withRouter(router)
        .currentRoute(noteShowLocation(noteRealm.id))
        .mount()

      await flushPromises()
      await vi.waitFor(() => {
        expect(wrapper.find('[data-test="keep-for-recall"]').exists()).toBe(
          true
        )
      })
    })

    it("does not render assimilation panel when settings are off", async () => {
      helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({ noteId: noteRealm.id })
        .withRouter(router)
        .currentRoute(noteShowLocation(noteRealm.id))
        .render()

      await flushPromises()
      await vi.waitFor(() => {
        const main = document.getElementById("main-note-content")
        expect(main).not.toBeNull()
      })

      expect(document.querySelector('[data-test="keep-for-recall"]')).toBeNull()
    })
  })
})

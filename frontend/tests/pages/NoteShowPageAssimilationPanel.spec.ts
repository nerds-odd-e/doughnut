import { useAssimilationView } from "@/composables/useAssimilationView"
import NoteShowPageWithNotebookSidebarLayout from "@tests/fixtures/NoteShowPageWithNotebookSidebarLayout.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { noteShowLocation } from "@/routes/noteShowLocation"
import {
  createNoteShowPageRouter,
  setupNoteShowPageAssimilationPanelMocks,
  withStubbedInnerWidth,
} from "@tests/pages/noteShowPageTestSupport"
import { assimilateButtonSelector } from "@tests/components/recall/assimilationPanelTestSupport"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"

describe("note show page inline assimilation panel", () => {
  let router: ReturnType<typeof createNoteShowPageRouter>
  let noteRealm: ReturnType<typeof setupNoteShowPageAssimilationPanelMocks>

  beforeEach(() => {
    router = createNoteShowPageRouter()
    noteRealm = setupNoteShowPageAssimilationPanelMocks()
  })

  it("keeps assimilation settings within main column when sidebar is open", async () => {
    await withStubbedInnerWidth(1024, async () => {
      useAssimilationView().openForNote(noteRealm.id)

      helper
        .component(NoteShowPageWithNotebookSidebarLayout)
        .withCurrentUser(makeMe.aUser.please())
        .withCleanStorage()
        .withProps({ noteId: noteRealm.id })
        .withRouter(router)
        .currentRoute(noteShowLocation(noteRealm.id))
        .render()

      await flushPromises()

      const settingsFooter = document.querySelector(
        'footer[aria-label="Assimilation settings"]'
      )
      const aside = document.querySelector("aside")
      expect(settingsFooter).not.toBeNull()
      expect(aside).not.toBeNull()

      const asideRect = aside!.getBoundingClientRect()
      const barRect = settingsFooter!.getBoundingClientRect()
      expect(barRect.left).toBeGreaterThanOrEqual(asideRect.right - 1)
    })
  })

  it("renders assimilate button when assimilation settings are on", async () => {
    useAssimilationView().openForNote(noteRealm.id)

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
      expect(wrapper.find(assimilateButtonSelector).exists()).toBe(true)
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

    expect(document.querySelector(assimilateButtonSelector)).toBeNull()
  })
})

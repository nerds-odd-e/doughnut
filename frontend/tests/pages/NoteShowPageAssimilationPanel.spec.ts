import { useAssimilationView } from "@/composables/useAssimilationView"
import {
  createNoteShowPageRouter,
  mainNoteContentEl,
  renderNoteShowPageWithoutSidebar,
  setupNoteShowPageAssimilationPanelMocks,
} from "@tests/pages/noteShowPageTestSupport"
import { assimilateButtonSelector } from "@tests/components/recall/assimilationPanelTestSupport"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"

describe("note show page inline assimilation panel", () => {
  const router = createNoteShowPageRouter()
  let noteRealm: ReturnType<typeof setupNoteShowPageAssimilationPanelMocks>

  beforeEach(() => {
    noteRealm = setupNoteShowPageAssimilationPanelMocks()
  })

  it("toggles assimilate button with assimilation settings", async () => {
    await renderNoteShowPageWithoutSidebar(router, noteRealm.id)
    await flushPromises()

    expect(mainNoteContentEl()).not.toBeNull()
    expect(document.querySelector(assimilateButtonSelector)).toBeNull()
    expect(
      document.querySelector('[data-testid="note-toolbar-panel-shell"]')
    ).toBeNull()

    useAssimilationView().openForNote(noteRealm.id)
    await flushPromises()

    expect(document.querySelector(assimilateButtonSelector)).not.toBeNull()
  })
})

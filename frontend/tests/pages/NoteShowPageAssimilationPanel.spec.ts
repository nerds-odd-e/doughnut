import { useAssimilationView } from "@/composables/useAssimilationView"
import {
  createNoteShowPageRouter,
  renderNoteShowPage,
  setupNoteShowPageAssimilationPanelMocks,
} from "@tests/pages/noteShowPageTestSupport"
import { assimilateButtonSelector } from "@tests/components/recall/assimilationPanelTestSupport"
import { beforeEach, describe, expect, it, vi } from "vitest"

describe("note show page inline assimilation panel", () => {
  const router = createNoteShowPageRouter()
  let noteRealm: ReturnType<typeof setupNoteShowPageAssimilationPanelMocks>

  beforeEach(() => {
    noteRealm = setupNoteShowPageAssimilationPanelMocks()
  })

  it("renders assimilate button when assimilation settings are on", async () => {
    useAssimilationView().openForNote(noteRealm.id)
    await renderNoteShowPage(router, noteRealm.id)

    await vi.waitFor(() => {
      expect(document.querySelector(assimilateButtonSelector)).not.toBeNull()
    })
  })

  it("does not render assimilation panel when settings are off", async () => {
    await renderNoteShowPage(router, noteRealm.id)

    await vi.waitFor(() => {
      expect(document.getElementById("main-note-content")).not.toBeNull()
    })

    expect(document.querySelector(assimilateButtonSelector)).toBeNull()
    expect(
      document.querySelector('[data-testid="note-toolbar-panel-shell"]')
    ).toBeNull()
  })
})

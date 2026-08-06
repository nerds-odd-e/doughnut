import { useAssimilationView } from "@/composables/useAssimilationView"
import {
  createNoteShowPageRouter,
  renderNoteShowPage,
  setupNoteShowPageAssimilationPanelMocks,
  withStubbedInnerWidth,
} from "@tests/pages/noteShowPageTestSupport"
import { assimilateButtonSelector } from "@tests/components/recall/assimilationPanelTestSupport"
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
      await renderNoteShowPage(router, noteRealm.id)

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
  })
})

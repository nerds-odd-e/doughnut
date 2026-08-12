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

  it("keeps assimilation settings in the shared toolbar panel when sidebar is open", async () => {
    await withStubbedInnerWidth(1024, async () => {
      useAssimilationView().openForNote(noteRealm.id)
      await renderNoteShowPage(router, noteRealm.id)

      const panelShell = document.querySelector(
        '[data-testid="note-toolbar-panel-shell"]'
      )
      const aside = document.querySelector("aside")
      expect(panelShell).not.toBeNull()
      expect(aside).not.toBeNull()

      const asideRect = aside!.getBoundingClientRect()
      const shellRect = panelShell!.getBoundingClientRect()
      expect(shellRect.left).toBeGreaterThanOrEqual(asideRect.right - 1)
      expect(aside!.contains(panelShell!)).toBe(false)
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
    expect(
      document.querySelector('[data-testid="note-toolbar-panel-shell"]')
    ).toBeNull()
  })
})

import makeMe from "donut-test-fixtures/makeMe"
import {
  installMockResizeObserver,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  mountNoteToolbar,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

describe("NoteToolbar assimilation panel", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    restoreNoteToolbarWidthMocks()
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    installMockResizeObserver()
    resetNoteToolbarTestState()
  })

  it("shows assimilation settings in the shared panel shell without a max-height cage", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm)
    useAssimilationView().openForNote(noteRealm.note.id)
    await flushPromises()

    const panelShell = wrapper.find('[data-testid="note-toolbar-panel-shell"]')
    expect(panelShell.exists()).toBe(true)
    const assimilationModes = panelShell.find(
      '[data-testid="note-assimilation-modes"]'
    )
    expect(assimilationModes.exists()).toBe(true)
    expect(
      assimilationModes.find(".max-h-\\[min\\(40vh\\,22rem\\)\\]").exists()
    ).toBe(false)

    useAssimilationView().dismiss()
    await flushPromises()

    expect(
      wrapper.find('[data-testid="note-toolbar-panel-shell"]').exists()
    ).toBe(false)
  })

  it("hides assimilation when audio opens and vice versa", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm)
    useAssimilationView().openForNote(noteRealm.note.id)
    await flushPromises()

    await wrapper
      .find(`button[title="${noteMoreOptionsTitles.audio}"]`)
      .trigger("click")
    await flushPromises()

    expect(
      wrapper.find('[data-testid="note-assimilation-modes"]').exists()
    ).toBe(false)
    expect(wrapper.find('button[title="Record Audio"]').exists()).toBe(true)

    useAssimilationView().openForNote(noteRealm.note.id)
    await flushPromises()

    expect(
      wrapper.find('[data-testid="note-assimilation-modes"]').exists()
    ).toBe(true)
    expect(wrapper.find('button[title="Record Audio"]').exists()).toBe(false)
  })
})

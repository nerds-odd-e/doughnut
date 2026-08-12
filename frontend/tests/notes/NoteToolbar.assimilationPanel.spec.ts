import makeMe from "doughnut-test-fixtures/makeMe"
import { installMockResizeObserver } from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  mountNoteToolbar,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

describe("NoteToolbar assimilation panel", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    installMockResizeObserver()
    resetNoteToolbarTestState()
  })

  it("shows assimilation settings in the shared panel shell when opened", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm)
    useAssimilationView().openForNote(noteRealm.note.id)
    await flushPromises()

    const panelShell = wrapper.find('[data-testid="note-toolbar-panel-shell"]')
    expect(panelShell.exists()).toBe(true)
    expect(
      panelShell.find('[data-testid="assimilation-settings"]').exists()
    ).toBe(true)

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

    expect(wrapper.find('[data-testid="assimilation-settings"]').exists()).toBe(
      true
    )

    await wrapper.find('button[title="Audio tools"]').trigger("click")
    await flushPromises()

    expect(wrapper.find('[data-testid="assimilation-settings"]').exists()).toBe(
      false
    )
    expect(
      wrapper.find('[data-testid="note-toolbar-panel-shell"]').exists()
    ).toBe(true)

    useAssimilationView().openForNote(noteRealm.note.id)
    await flushPromises()

    expect(wrapper.find('[data-testid="assimilation-settings"]').exists()).toBe(
      true
    )
    expect(wrapper.find('button[title="Record Audio"]').exists()).toBe(false)
  })

  it("does not cage assimilation settings in a half-page max-height scroll area", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm)
    useAssimilationView().openForNote(noteRealm.note.id)
    await flushPromises()

    expect(
      wrapper
        .find('[data-testid="assimilation-settings"]')
        .find(".max-h-\\[min\\(40vh\\,22rem\\)\\]")
        .exists()
    ).toBe(false)
  })
})

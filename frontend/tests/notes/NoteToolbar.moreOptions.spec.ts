import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  allMoreOptionsFitNavWidth,
  installMockResizeObserver,
  layoutNoteToolbar,
  overflowTogglesNavWidth,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mountNoteToolbar,
  noteToolbarAction,
  noteToolbarProps,
  overflowMenuItem,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { useNoteToolbarPanel } from "@/composables/useNoteToolbarPanel"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

const aiMarkdownStub = { markdown: "# AI context\n\nHello **world**." }
const titles = noteMoreOptionsTitles

describe("NoteToolbar more options", () => {
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

  it("copies export markdown while keeping the export dialog open", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    mockSdkService(NoteController, "getAiContextMarkdown", aiMarkdownStub)
    const writeText = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal("navigator", {
      ...navigator,
      clipboard: { writeText },
    })

    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, allMoreOptionsFitNavWidth())

    const exportBtn = wrapper.find(`button[title="${titles.export}"]`)
    expect(exportBtn.exists()).toBe(true)
    await exportBtn.trigger("click")

    const dialog = document.querySelector("dialog") as HTMLDialogElement
    expect(dialog?.open).toBe(true)

    const copyBtn = document.querySelector(
      '[data-testid="copy-ai-context-md-btn"]'
    ) as HTMLButtonElement
    expect(copyBtn).toBeTruthy()
    copyBtn.click()
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith(
      expect.stringContaining("AI context")
    )
    expect(dialog.open).toBe(true)
  })

  it("closes more options dialog when note id changes", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())

    await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
    await flushPromises()

    expect(
      document.querySelector("[data-dropdown-portal-panel]")
    ).not.toBeNull()

    const newNote = makeMe.aNoteRealm.title("New Note").please()
    await wrapper.setProps(noteToolbarProps(newNote))
    await flushPromises()

    const details = wrapper.find("[data-auto-collapse-dropdown]")
    expect((details.element as HTMLDetailsElement).open).toBe(false)
  })

  it("toggles the audio tools panel from the inline button and overflow menu", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, allMoreOptionsFitNavWidth())

    const audioToolsButton = wrapper.find(`button[title="${titles.audio}"]`)
    expect(audioToolsButton.exists()).toBe(true)
    expect(audioToolsButton.classes()).not.toContain("daisy-btn-soft")
    expect(audioToolsButton.classes()).not.toContain("daisy-btn-primary")
    expect(
      wrapper.find('[data-testid="note-toolbar-panel-shell"]').exists()
    ).toBe(false)

    await audioToolsButton.trigger("click")
    await flushPromises()

    expect(
      wrapper.find('[data-testid="note-toolbar-panel-shell"]').exists()
    ).toBe(true)
    expect(useNoteToolbarPanel().isAudioOpen.value).toBe(true)
    expect(audioToolsButton.classes()).toContain("daisy-btn-soft")
    expect(audioToolsButton.classes()).toContain("daisy-btn-primary")
    expect(audioToolsButton.attributes("aria-pressed")).toBe("true")

    await audioToolsButton.trigger("click")
    await flushPromises()

    expect(
      wrapper.find('[data-testid="note-toolbar-panel-shell"]').exists()
    ).toBe(false)
    expect(audioToolsButton.classes()).not.toContain("daisy-btn-soft")
    expect(audioToolsButton.classes()).not.toContain("daisy-btn-primary")
    expect(audioToolsButton.attributes("aria-pressed")).toBe("false")

    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
    await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
    await flushPromises()

    const overflowAudio = overflowMenuItem(titles.audio) as HTMLButtonElement
    expect(overflowAudio).toBeTruthy()
    overflowAudio.click()
    await flushPromises()

    expect(
      wrapper.find('[data-testid="note-toolbar-panel-shell"]').exists()
    ).toBe(true)
    expect(useNoteToolbarPanel().isAudioOpen.value).toBe(true)
    expect(document.querySelector("[data-dropdown-portal-panel]")).toBeNull()
  })
})

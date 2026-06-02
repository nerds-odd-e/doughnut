import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  installMockResizeObserver,
  narrowNoteToolbarNavWidth,
  setNoteToolbarNavWidth,
  wideNoteToolbarNavWidth,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mountNoteToolbar,
  noteToolbarProps,
} from "@tests/notes/noteToolbarTestHelpers"
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
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    installMockResizeObserver()
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
    setNoteToolbarNavWidth(wrapper, narrowNoteToolbarNavWidth)
    await flushPromises()

    await wrapper.find(`[title="${titles.overflowMenu}"]`).trigger("click")
    await flushPromises()

    const exportBtn = document.querySelector(
      `button[title="${titles.export}"]`
    ) as HTMLButtonElement
    expect(exportBtn).toBeTruthy()
    exportBtn.click()
    await flushPromises()

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

  it("displays menu items when dropdown is open", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    setNoteToolbarNavWidth(wrapper, narrowNoteToolbarNavWidth)
    await flushPromises()

    await wrapper.find(`[title="${titles.overflowMenu}"]`).trigger("click")
    await flushPromises()

    expect(wrapper.findComponent(NoteMoreOptionsForm).exists()).toBe(true)
    expect(
      document.querySelector("[data-dropdown-portal-panel]")
    ).not.toBeNull()
    expect(
      document.querySelector(`button[title="${titles.questions}"]`)
    ).not.toBeNull()
  })

  it("closes more options dialog when note id changes", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    setNoteToolbarNavWidth(wrapper, narrowNoteToolbarNavWidth)
    await flushPromises()

    await wrapper.find(`[title="${titles.overflowMenu}"]`).trigger("click")
    await flushPromises()

    expect(wrapper.findComponent(NoteMoreOptionsForm).exists()).toBe(true)

    const newNote = makeMe.aNoteRealm.title("New Note").please()
    await wrapper.setProps(noteToolbarProps(newNote))
    await flushPromises()

    const details = wrapper.find("[data-auto-collapse-dropdown]")
    expect((details.element as HTMLDetailsElement).open).toBe(false)
  })

  it("shows more options in dropdown when toolbar is narrow", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    setNoteToolbarNavWidth(wrapper, narrowNoteToolbarNavWidth)
    await flushPromises()

    expect(wrapper.find(`[title="${titles.overflowMenu}"]`).exists()).toBe(true)
    expect(wrapper.find(`button[title="${titles.export}"]`).exists()).toBe(
      false
    )
  })

  it("shows more options actions inline when toolbar is wide", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    setNoteToolbarNavWidth(wrapper, wideNoteToolbarNavWidth)
    await flushPromises()

    expect(wrapper.find(`[title="${titles.overflowMenu}"]`).exists()).toBe(
      false
    )
    expect(wrapper.find(`button[title="${titles.export}"]`).exists()).toBe(true)
    expect(
      wrapper.find(`button[title="${titles.assimilation}"]`).exists()
    ).toBe(true)
  })
})

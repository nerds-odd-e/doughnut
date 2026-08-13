import {
  allMoreOptionsFitNavWidth,
  deleteOverflowNavWidth,
  editOverflowNavWidth,
  exportOverflowNavWidth,
  installMockResizeObserver,
  layoutNoteToolbar,
  remainingMoreOptionsNavWidth,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  noteMoreOptionsTitles,
  noteToolbarEditTitles,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mountOverflowToolbar,
  noteToolbarAction,
  overflowMenuItem,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

const titles = noteMoreOptionsTitles

describe("NoteToolbar more-options overflow", () => {
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

  it("keeps all more-options on the bar when they fit", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, allMoreOptionsFitNavWidth())

    expect(noteToolbarAction(wrapper, titles.overflowMenu).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.questions).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.assimilation).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.delete).exists()).toBe(true)
  })

  it("hides delete first when the full more-options set does not fit", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, deleteOverflowNavWidth())

    expect(noteToolbarAction(wrapper, titles.overflowMenu).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.delete).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.questions).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.assimilation).exists()).toBe(true)
    expect(
      noteToolbarAction(wrapper, noteToolbarEditTitles.markdown).exists()
    ).toBe(true)
  })

  it("lists only overflowed actions in more options", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, deleteOverflowNavWidth())

    await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
    await flushPromises()

    expect(overflowMenuItem(titles.delete)).not.toBeNull()
    expect(overflowMenuItem(noteToolbarEditTitles.markdown)).toBeNull()
    expect(overflowMenuItem(titles.export)).toBeNull()
    expect(overflowMenuItem(titles.questions)).toBeNull()
    expect(overflowMenuItem(titles.audio)).toBeNull()
    expect(overflowMenuItem(titles.assimilation)).toBeNull()
  })

  it("hides off-state assimilation then audio as the bar shrinks further", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, remainingMoreOptionsNavWidth(7) - 1)

    expect(noteToolbarAction(wrapper, titles.delete).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.assimilation).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.questions).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(true)
  })

  it("keeps Edit on the bar after Export has overflowed", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, exportOverflowNavWidth())

    expect(
      noteToolbarAction(wrapper, noteToolbarEditTitles.markdown).exists()
    ).toBe(true)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(false)
  })

  it("moves Edit into more options when the bar is tighter than Export overflow", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, editOverflowNavWidth())

    expect(
      noteToolbarAction(wrapper, noteToolbarEditTitles.markdown).exists()
    ).toBe(false)
    expect(noteToolbarAction(wrapper, titles.conversation).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.wiki).isVisible()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.new).isVisible()).toBe(true)

    await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
    await flushPromises()

    const editItem = overflowMenuItem(noteToolbarEditTitles.markdown)
    expect(editItem).not.toBeNull()
    expect(editItem?.textContent).toContain(noteToolbarEditTitles.markdown)
    expect(overflowMenuItem(titles.export)).not.toBeNull()
  })

  it("emits edit-as-markdown from the overflow Edit row", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, editOverflowNavWidth())

    await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
    await flushPromises()

    overflowMenuItem(noteToolbarEditTitles.markdown)?.click()
    await flushPromises()

    expect(wrapper.emitted("edit-as-markdown")).toEqual([[true]])
  })

  it("still toggles edit mode with m when Edit is in more options", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, editOverflowNavWidth())

    document.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "m",
        code: "KeyM",
        bubbles: true,
        cancelable: true,
      })
    )
    await flushPromises()

    expect(wrapper.emitted("edit-as-markdown")).toEqual([[true]])
  })
})

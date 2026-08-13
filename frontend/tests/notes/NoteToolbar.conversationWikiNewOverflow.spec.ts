import {
  conversationOverflowNavWidth,
  installMockResizeObserver,
  layoutNoteToolbar,
  overflowOnlyNavWidth,
  overflowTogglesNavWidth,
  pinnedToggleOnlyNavWidth,
  restoreNoteToolbarWidthMocks,
  wikiOverflowNavWidth,
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
import { setupNoteNewFormSdkMocks } from "@tests/notes/noteNewFormTestSupport"
import { setupSearchFormSdkMocks } from "@tests/wiki-link-or-relationship/searchDialogTestSupport"
import { screen } from "@testing-library/vue"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

const titles = noteMoreOptionsTitles

describe("NoteToolbar Conversation, Wiki, and New overflow", () => {
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

  it("moves Conversation into more options before Wiki or New", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, conversationOverflowNavWidth())

    expect(noteToolbarAction(wrapper, titles.conversation).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.wiki).isVisible()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.new).isVisible()).toBe(true)

    await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
    await flushPromises()

    expect(overflowMenuItem(titles.conversation)).not.toBeNull()
    expect(overflowMenuItem(titles.wiki)).toBeNull()
    expect(overflowMenuItem(titles.new)).toBeNull()
  })

  it("keeps only the on-toggle and more options on an extremely narrow bar", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
    await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
    await flushPromises()
    overflowMenuItem(titles.audio)?.click()
    await flushPromises()
    await layoutNoteToolbar(wrapper, pinnedToggleOnlyNavWidth())

    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.overflowMenu).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.conversation).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.wiki).isVisible()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.new).isVisible()).toBe(false)
    expect(
      noteToolbarAction(wrapper, noteToolbarEditTitles.markdown).exists()
    ).toBe(false)
  })

  it("keeps only more options when nothing is pinned and everything overflowed", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, overflowOnlyNavWidth())

    expect(noteToolbarAction(wrapper, titles.overflowMenu).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.conversation).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.wiki).isVisible()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.new).isVisible()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(false)
  })

  it("still opens wiki search when Wiki is in more options", async () => {
    setupSearchFormSdkMocks()
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, wikiOverflowNavWidth())

    expect(noteToolbarAction(wrapper, titles.wiki).isVisible()).toBe(false)
    expect(screen.queryByPlaceholderText("Search")).toBeNull()

    document.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "f",
        code: "KeyF",
        ctrlKey: true,
        shiftKey: true,
        bubbles: true,
        cancelable: true,
      })
    )
    await flushPromises()

    expect(await screen.findByPlaceholderText("Search")).toBeInTheDocument()
  })

  it("still opens new note when New is in more options", async () => {
    setupNoteNewFormSdkMocks()
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, overflowOnlyNavWidth())

    expect(noteToolbarAction(wrapper, titles.new).isVisible()).toBe(false)
    expect(screen.queryByTestId("note-new-form")).toBeNull()

    document.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "n",
        code: "KeyN",
        bubbles: true,
        cancelable: true,
      })
    )
    await flushPromises()

    expect(await screen.findByTestId("note-new-form")).toBeInTheDocument()
  })
})

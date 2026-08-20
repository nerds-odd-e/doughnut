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
  dispatchDocumentKey,
  mountOverflowToolbar,
  noteToolbarAction,
  noteToolbarNewDisplayed,
  noteToolbarWikiHidden,
  openNoteToolbarOverflowMenu,
  overflowMenuItem,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { setupNoteNewFormSdkMocks } from "@tests/notes/noteNewFormTestSupport"
import { setupSearchFormSdkMocks } from "@tests/wiki-link-or-relationship/searchDialogTestSupport"
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

  it("overflows Conversation before Wiki/New, and shortcuts still open them", async () => {
    setupSearchFormSdkMocks()
    setupNoteNewFormSdkMocks()
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, conversationOverflowNavWidth())

    expect(noteToolbarAction(wrapper, titles.conversation).exists()).toBe(false)
    expect(noteToolbarWikiHidden(wrapper)).toBe(false)
    expect(noteToolbarNewDisplayed(wrapper)).toBe(true)

    await openNoteToolbarOverflowMenu(wrapper)
    expect(overflowMenuItem(titles.conversation)).not.toBeNull()
    expect(overflowMenuItem(titles.wiki)).toBeNull()
    expect(overflowMenuItem(titles.new)).toBeNull()
    await openNoteToolbarOverflowMenu(wrapper)

    await layoutNoteToolbar(wrapper, wikiOverflowNavWidth())
    expect(noteToolbarWikiHidden(wrapper)).toBe(true)
    expect(document.querySelector('input[placeholder="Search"]')).toBeNull()

    dispatchDocumentKey({
      key: "f",
      code: "KeyF",
      ctrlKey: true,
      shiftKey: true,
    })
    await flushPromises()
    expect(document.querySelector('input[placeholder="Search"]')).not.toBeNull()

    dispatchDocumentKey({ key: "Escape" })
    await flushPromises()
    expect(document.querySelector('input[placeholder="Search"]')).toBeNull()

    await layoutNoteToolbar(wrapper, overflowOnlyNavWidth())
    expect(noteToolbarNewDisplayed(wrapper)).toBe(false)
    expect(document.querySelector('[data-testid="note-new-form"]')).toBeNull()

    dispatchDocumentKey({ key: "n", code: "KeyN" })
    await flushPromises()
    expect(
      document.querySelector('[data-testid="note-new-form"]')
    ).not.toBeNull()
  })

  it("keeps a pinned on-toggle then only more options when nothing is pinned", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())
    await openNoteToolbarOverflowMenu(wrapper)
    overflowMenuItem(titles.audio)?.click()
    await flushPromises()
    await layoutNoteToolbar(wrapper, pinnedToggleOnlyNavWidth())

    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.overflowMenu).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.conversation).exists()).toBe(false)
    expect(noteToolbarWikiHidden(wrapper)).toBe(true)
    expect(noteToolbarNewDisplayed(wrapper)).toBe(false)
    expect(
      noteToolbarAction(wrapper, noteToolbarEditTitles.markdown).exists()
    ).toBe(false)

    await noteToolbarAction(wrapper, titles.audio).trigger("click")
    await flushPromises()
    await layoutNoteToolbar(wrapper, overflowOnlyNavWidth())

    expect(noteToolbarAction(wrapper, titles.overflowMenu).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.conversation).exists()).toBe(false)
    expect(noteToolbarWikiHidden(wrapper)).toBe(true)
    expect(noteToolbarNewDisplayed(wrapper)).toBe(false)
    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(false)
  })
})

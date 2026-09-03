import {
  conversationOverflowNavWidth,
  installMockResizeObserver,
  layoutNoteToolbar,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  openNoteToolbarOverflowMenu,
  overflowMenuItem,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { mountNoteToolbarAt } from "@tests/notes/noteToolbarRouteMount"
import { notebookSidebarClosedPlugin } from "@tests/helpers/notebookSidebarTestProvide"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

describe("NoteToolbar conversation overflow", () => {
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

  it("keeps the current property location when starting a conversation from overflow", async () => {
    const mounted = await mountNoteToolbarAt(
      (noteId) => notePropertyLocation(noteId, "topic"),
      { plugin: notebookSidebarClosedPlugin() }
    )
    wrapper = mounted.wrapper
    const { router, noteRealm } = mounted
    await layoutNoteToolbar(wrapper, conversationOverflowNavWidth())
    await openNoteToolbarOverflowMenu(wrapper)
    overflowMenuItem(noteMoreOptionsTitles.conversation)!.click()
    await flushPromises()

    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteRealm.note.id, "topic")
    )
    expect(router.currentRoute.value.query).toEqual({ conversation: "true" })
  })
})

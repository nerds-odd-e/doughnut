import makeMe from "doughnut-test-fixtures/makeMe"
import {
  allMoreOptionsFitNavWidth,
  deleteOverflowNavWidth,
  installMockResizeObserver,
  layoutNoteToolbar,
  noteToolbarActionWidth,
  noteToolbarOverflowButtonWidth,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mountNoteToolbar,
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
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, allMoreOptionsFitNavWidth())

    expect(noteToolbarAction(wrapper, titles.overflowMenu).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.questions).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.assimilation).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.delete).exists()).toBe(true)
  })

  it("hides delete first when the full more-options set does not fit", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, deleteOverflowNavWidth())

    expect(noteToolbarAction(wrapper, titles.overflowMenu).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.delete).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.questions).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.assimilation).exists()).toBe(true)
    expect(wrapper.find('[title="Edit as markdown (m)"]').exists()).toBe(true)
  })

  it("lists only overflowed actions in more options", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, deleteOverflowNavWidth())

    await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
    await flushPromises()

    expect(overflowMenuItem(titles.delete)).not.toBeNull()
    expect(overflowMenuItem(titles.export)).toBeNull()
    expect(overflowMenuItem(titles.questions)).toBeNull()
    expect(overflowMenuItem(titles.audio)).toBeNull()
    expect(overflowMenuItem(titles.assimilation)).toBeNull()
  })

  it("hides off-state assimilation then audio as the bar shrinks further", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(
      wrapper,
      noteToolbarActionWidth * 3 + noteToolbarOverflowButtonWidth - 1
    )

    expect(noteToolbarAction(wrapper, titles.delete).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.assimilation).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.questions).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(true)
  })
})

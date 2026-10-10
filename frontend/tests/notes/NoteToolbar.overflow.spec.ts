import makeMe from "donut-test-fixtures/makeMe"
import {
  allMoreOptionsFitNavWidth,
  deleteOverflowNavWidth,
  editOverflowNavWidth,
  exportOverflowNavWidth,
  installMockResizeObserver,
  layoutNoteToolbar,
  overflowTogglesNavWidth,
  remainingMoreOptionsNavWidth,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  noteMoreOptionsTitles,
  noteToolbarEditTitles,
  noteVoiceInputTitles,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mountNoteToolbar,
  mountOverflowToolbar,
  noteToolbarAction,
  noteToolbarProps,
  openNoteToolbarOverflowMenu,
  overflowMenuItem,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

const titles = noteMoreOptionsTitles

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return audioRecorderMockExports()
})

vi.mock("@/models/wakeLocker", async () => {
  const { wakeLockerMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return wakeLockerMockExports()
})

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
    expect(noteToolbarAction(wrapper, titles.mcqs).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.voiceInput).isVisible()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.assimilation).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.delete).exists()).toBe(true)
  })

  it("overflows delete first and lists only that action in more options", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, deleteOverflowNavWidth())

    expect(noteToolbarAction(wrapper, titles.overflowMenu).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.delete).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.mcqs).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.voiceInput).isVisible()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.assimilation).exists()).toBe(true)
    expect(
      noteToolbarAction(wrapper, noteToolbarEditTitles.markdown).exists()
    ).toBe(true)

    await openNoteToolbarOverflowMenu(wrapper)

    expect(overflowMenuItem(titles.delete)).not.toBeNull()
    expect(overflowMenuItem(noteToolbarEditTitles.markdown)).toBeNull()
    expect(overflowMenuItem(titles.export)).toBeNull()
    expect(overflowMenuItem(titles.mcqs)).toBeNull()
    expect(overflowMenuItem(titles.voiceInput)).toBeNull()
    expect(overflowMenuItem(titles.assimilation)).toBeNull()
  })

  it("hides off-state assimilation then idle voice input as the bar shrinks further", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, remainingMoreOptionsNavWidth(7) - 1)

    expect(noteToolbarAction(wrapper, titles.delete).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.assimilation).exists()).toBe(false)
    expect(noteToolbarAction(wrapper, titles.voiceInput).isVisible()).toBe(
      false
    )
    expect(noteToolbarAction(wrapper, titles.mcqs).exists()).toBe(true)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(true)
  })

  it("overflows Export then Edit, and Edit still works from the menu", async () => {
    wrapper = await mountOverflowToolbar()
    await layoutNoteToolbar(wrapper, exportOverflowNavWidth())

    expect(
      noteToolbarAction(wrapper, noteToolbarEditTitles.markdown).exists()
    ).toBe(true)
    expect(noteToolbarAction(wrapper, titles.export).exists()).toBe(false)

    await layoutNoteToolbar(wrapper, editOverflowNavWidth())

    expect(
      noteToolbarAction(wrapper, noteToolbarEditTitles.markdown).exists()
    ).toBe(false)

    await openNoteToolbarOverflowMenu(wrapper)

    const editItem = overflowMenuItem(noteToolbarEditTitles.markdown)
    expect(editItem).not.toBeNull()
    expect(editItem?.textContent).toContain(noteToolbarEditTitles.markdown)
    expect(overflowMenuItem(titles.export)).not.toBeNull()

    editItem?.click()
    await flushPromises()

    expect(wrapper.emitted("edit-as-markdown")).toEqual([[true]])
  })

  it("pins Voice input on a narrow toolbar while recording then returns it to overflow when stopped", async () => {
    wrapper = await mountNoteToolbar(makeMe.aNoteRealm.please())
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())

    await openNoteToolbarOverflowMenu(wrapper)
    overflowMenuItem(titles.voiceInput)!.click()
    await flushPromises()

    const stopButton = noteToolbarAction(wrapper, noteVoiceInputTitles.stop)
    expect(stopButton.isVisible()).toBe(true)
    expect(stopButton.attributes("aria-pressed")).toBe("true")
    expect(document.querySelector("[data-dropdown-portal-panel]")).toBeNull()

    await stopButton.trigger("click")
    await flushPromises()

    await openNoteToolbarOverflowMenu(wrapper)
    expect(overflowMenuItem(titles.voiceInput)).not.toBeNull()
  })

  it("closes more options dialog when note id changes", async () => {
    wrapper = await mountNoteToolbar(makeMe.aNoteRealm.please())
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())

    await openNoteToolbarOverflowMenu(wrapper)
    expect(
      document.querySelector("[data-dropdown-portal-panel]")
    ).not.toBeNull()

    await wrapper.setProps(noteToolbarProps(makeMe.aNoteRealm.please()))
    await flushPromises()

    const details = wrapper.find("[data-auto-collapse-dropdown]")
    expect((details.element as HTMLDetailsElement).open).toBe(false)
  })
})

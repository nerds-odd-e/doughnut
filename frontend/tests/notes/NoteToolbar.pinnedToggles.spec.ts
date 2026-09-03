import makeMe from "donut-test-fixtures/makeMe"
import {
  installMockResizeObserver,
  layoutNoteToolbar,
  overflowTogglesNavWidth,
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

async function turnOnFromOverflow(wrapper: VueWrapper, title: string) {
  await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
  await flushPromises()
  const button = overflowMenuItem(title) as HTMLButtonElement
  expect(button, `${title} should be in the overflow menu`).toBeTruthy()
  button.click()
  await flushPromises()
}

async function expectPinnedOnNarrowToolbar(
  wrapper: VueWrapper,
  pinnedTitle: string,
  menuTitle: string
) {
  const pinned = noteToolbarAction(wrapper, pinnedTitle)
  expect(pinned.exists()).toBe(true)
  expect(pinned.classes()).toContain("daisy-btn-soft")
  expect(pinned.classes()).toContain("daisy-btn-primary")
  expect(pinned.classes()).toContain("shrink-0")
  expect(pinned.attributes("aria-pressed")).toBe("true")

  await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
  await flushPromises()

  expect(overflowMenuItem(pinnedTitle)).toBeNull()
  expect(overflowMenuItem(menuTitle)).not.toBeNull()

  await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
  await flushPromises()
}

describe("NoteToolbar pinned on-state toggles", () => {
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

  it("pins audio on a narrow toolbar then returns it to overflow when turned off", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    wrapper = await mountNoteToolbar(noteRealm)
    await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())

    await turnOnFromOverflow(wrapper, titles.audio)
    await expectPinnedOnNarrowToolbar(
      wrapper,
      titles.audio,
      titles.assimilation
    )

    await noteToolbarAction(wrapper, titles.audio).trigger("click")
    await flushPromises()

    expect(noteToolbarAction(wrapper, titles.audio).exists()).toBe(false)

    await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
    await flushPromises()
    expect(overflowMenuItem(titles.audio)).not.toBeNull()
  })
})

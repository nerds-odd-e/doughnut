import makeMe from "doughnut-test-fixtures/makeMe"
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

const pinnedToggleCases = [
  {
    name: "audio",
    title: titles.audio,
    menuTitle: titles.assimilation,
  },
  {
    name: "assimilation",
    title: titles.assimilation,
    menuTitle: titles.audio,
  },
] as const

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

  it.each(pinnedToggleCases)(
    "pins $name on a narrow toolbar then returns it to overflow when turned off",
    async ({ title, menuTitle }) => {
      const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
      wrapper = await mountNoteToolbar(noteRealm)
      await layoutNoteToolbar(wrapper, overflowTogglesNavWidth())

      await turnOnFromOverflow(wrapper, title)
      await expectPinnedOnNarrowToolbar(wrapper, title, menuTitle)

      await noteToolbarAction(wrapper, title).trigger("click")
      await flushPromises()

      expect(noteToolbarAction(wrapper, title).exists()).toBe(false)

      await noteToolbarAction(wrapper, titles.overflowMenu).trigger("click")
      await flushPromises()
      expect(overflowMenuItem(title)).not.toBeNull()
    }
  )
})

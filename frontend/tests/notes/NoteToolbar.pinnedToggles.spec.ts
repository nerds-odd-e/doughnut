import makeMe from "doughnut-test-fixtures/makeMe"
import {
  installMockResizeObserver,
  narrowNoteToolbarNavWidth,
  setNoteToolbarNavWidth,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mountNoteToolbar,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

const titles = noteMoreOptionsTitles

function toolbarToggle(wrapper: VueWrapper, title: string) {
  return wrapper.find(`[data-note-toolbar] button[title="${title}"]`)
}

function overflowToggle(title: string) {
  return document.querySelector(
    `[data-dropdown-portal-panel] button[title="${title}"]`
  )
}

async function turnOnFromOverflow(wrapper: VueWrapper, title: string) {
  await wrapper.find(`[title="${titles.overflowMenu}"]`).trigger("click")
  await flushPromises()
  const button = overflowToggle(title) as HTMLButtonElement
  expect(button, `${title} should be in the overflow menu`).toBeTruthy()
  button.click()
  await flushPromises()
}

async function expectPinnedOnNarrowToolbar(
  wrapper: VueWrapper,
  pinnedTitle: string,
  menuTitle: string
) {
  const pinned = toolbarToggle(wrapper, pinnedTitle)
  expect(pinned.exists()).toBe(true)
  expect(pinned.classes()).toContain("daisy-btn-soft")
  expect(pinned.classes()).toContain("daisy-btn-primary")
  expect(pinned.classes()).toContain("shrink-0")
  expect(pinned.attributes("aria-pressed")).toBe("true")

  await wrapper.find(`[title="${titles.overflowMenu}"]`).trigger("click")
  await flushPromises()

  expect(overflowToggle(pinnedTitle)).toBeNull()
  expect(overflowToggle(menuTitle)).not.toBeNull()
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
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    installMockResizeObserver()
    resetNoteToolbarTestState()
  })

  it.each(pinnedToggleCases)(
    "pins $name on a narrow toolbar and omits it from overflow",
    async ({ title, menuTitle }) => {
      const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
      wrapper = await mountNoteToolbar(noteRealm)
      setNoteToolbarNavWidth(wrapper, narrowNoteToolbarNavWidth)
      await flushPromises()

      await turnOnFromOverflow(wrapper, title)
      await expectPinnedOnNarrowToolbar(wrapper, title, menuTitle)
    }
  )

  it.each(pinnedToggleCases)(
    "returns $name to the overflow menu when the pinned toolbar toggle is turned off",
    async ({ title }) => {
      const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
      wrapper = await mountNoteToolbar(noteRealm)
      setNoteToolbarNavWidth(wrapper, narrowNoteToolbarNavWidth)
      await flushPromises()

      await turnOnFromOverflow(wrapper, title)
      await toolbarToggle(wrapper, title).trigger("click")
      await flushPromises()

      expect(toolbarToggle(wrapper, title).exists()).toBe(false)

      await wrapper.find(`[title="${titles.overflowMenu}"]`).trigger("click")
      await flushPromises()
      expect(overflowToggle(title)).not.toBeNull()
    }
  )
})

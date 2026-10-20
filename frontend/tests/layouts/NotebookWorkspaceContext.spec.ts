import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import {
  chooseFromMore,
  clickPageBarCommand,
  commandsSitInPageBar,
  pageBarCommand,
  pageBarEl,
} from "./notebookWorkspaceHeaderTestSupport"

import {
  innerFolder,
  mountDeepNote,
  notebookLocationControl,
} from "./notebookWorkspaceContextTestSupport"

const titles = noteMoreOptionsTitles
const rect = (el: Element) => el.getBoundingClientRect()
const undoEl = () => document.querySelector('[title="undo edit title"]')!
const contentsToggle = () =>
  document.querySelector<HTMLElement>(
    '[aria-label="Show sidebar"], [aria-label="Hide sidebar"]'
  )!
const contentsPane = () => document.querySelector("aside")!

/** Visible note commands (including More) and Undo all inside the bar and the window. */
function toolsFitInBar(width: number) {
  const undo = rect(undoEl())
  const bar = rect(pageBarEl())
  return (
    commandsSitInPageBar(width) &&
    undo.right <= width &&
    undo.top >= bar.top &&
    undo.bottom <= bar.bottom
  )
}

describe("Notebook workspace folder context", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    useAssimilationView().dismiss()
  })

  afterEach(() => {
    wrapper.unmount()
    document.body.innerHTML = ""
  })

  it.each([
    { width: 320, overflows: true },
    { width: 390, overflows: true },
    { width: 767, overflows: false },
  ])(
    "shows a long trail below the bar and above the note at $width px while tools keep their space",
    async ({ width, overflows }) => {
      ;({ wrapper } = await mountDeepNote(width))
      if (pageBarCommand(titles.assimilation)) {
        await clickPageBarCommand(titles.assimilation)
      } else {
        await chooseFromMore(titles.assimilation)
      }

      await vi.waitFor(() => {
        const context = rect(notebookLocationControl())
        expect(pageBarEl().contains(notebookLocationControl())).toBe(false)
        expect(context.top).toBeGreaterThanOrEqual(rect(pageBarEl()).bottom)
        expect(context.bottom).toBeLessThanOrEqual(
          rect(document.querySelector("#main-note-content")!).top
        )
        expect(context.right).toBeLessThanOrEqual(width)
        expect(
          pageBarCommand(titles.assimilation)?.getAttribute("aria-pressed")
        ).toBe("true")
        expect(pageBarCommand(titles.overflowMenu) !== null).toBe(overflows)
        expect(toolsFitInBar(width)).toBe(true)
      })
    }
  )

  it.each([768, 1280])(
    "shows notebook identity above the docked tree at %ipx while tools keep their space",
    async (width) => {
      ;({ wrapper } = await mountDeepNote(width))

      await vi.waitFor(() => {
        const context = rect(notebookLocationControl())
        const bar = rect(pageBarEl())
        expect(context.top).toBeGreaterThanOrEqual(bar.bottom)
        expect(context.bottom).toBeLessThanOrEqual(
          rect(document.querySelector(".sidebar-tree-scroll")!).top
        )
        expect(contentsPane().contains(notebookLocationControl())).toBe(true)
        const notebookName =
          notebookLocationControl().querySelector(".truncate")!
        expect(rect(notebookName).width).toBeGreaterThan(180)
        expect(toolsFitInBar(width)).toBe(true)
      })
    }
  )

  it("opens contents as a drawer from the bar's bottom on a phone", async () => {
    ;({ wrapper } = await mountDeepNote(390))
    expect(contentsToggle().getAttribute("aria-label")).toBe("Show sidebar")

    await page.elementLocator(contentsToggle()).click()
    await flushPromises()

    await vi.waitFor(() => {
      expect(getComputedStyle(contentsPane()).position).toBe("fixed")
      expect(rect(contentsPane()).top).toBe(rect(pageBarEl()).bottom)
      expect(rect(contentsPane()).right).toBeGreaterThan(0)
    })
  })

  it("docks contents beside the note and toggles it on a wide window", async () => {
    ;({ wrapper } = await mountDeepNote(1280))
    expect(getComputedStyle(contentsPane()).position).toBe("relative")
    expect(rect(contentsPane()).top).toBeGreaterThanOrEqual(
      rect(pageBarEl()).bottom
    )

    await page.elementLocator(contentsToggle()).click()
    await flushPromises()

    expect(getComputedStyle(contentsPane()).display).toBe("none")
  })

  describe("New note at 1280px", () => {
    const formTarget = () =>
      document.querySelector('[data-testid="note-new-form"]')?.textContent
    const expected = `Adds to folder "${innerFolder.name}".`

    beforeEach(async () => {
      ;({ wrapper } = await mountDeepNote(1280))
    })

    it("adds to the note's folder from the sidebar", async () => {
      await page
        .elementLocator(
          document.querySelector(
            `[data-note-sidebar-toolbar] [title="${titles.new}"]`
          )!
        )
        .click()
      await flushPromises()
      expect(formTarget()).toContain(expected)
    })

    it("adds to the note's folder from the note tools when contents are hidden", async () => {
      await page.elementLocator(contentsToggle()).click()
      await flushPromises()
      await vi.waitFor(() => expect(pageBarCommand(titles.new)).not.toBeNull())
      await page.elementLocator(pageBarCommand(titles.new)!).click()
      await flushPromises()
      expect(formTarget()).toContain(expected)
    })
  })
})

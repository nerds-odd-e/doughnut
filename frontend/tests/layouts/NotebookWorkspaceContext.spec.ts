import { NotebookFolderController } from "@generated/donut-backend-api/sdk.gen"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  chooseFromMore,
  clickPageBarCommand,
  commandsSitInPageBar,
  mountNotebookWorkspace,
  noteCommandEls,
  pageBarCommand,
  pageBarEl,
} from "./notebookWorkspaceHeaderTestSupport"

const titles = noteMoreOptionsTitles
const longNotebook = "An exceptionally long notebook name about orchards"
const parentFolder = {
  id: 501,
  name: "Fruit trees of the north",
}
const innerFolder = {
  id: 502,
  name: "Apples pears and quinces grown on dwarfing rootstocks",
}

function deepNote() {
  return makeMe.aNoteRealm
    .title("Orchard")
    .notebookName(longNotebook)
    .ancestorFolders([parentFolder, innerFolder])
    .please()
}

const contextEl = () =>
  document.querySelector<HTMLElement>(".daisy-breadcrumbs")!
const rect = (el: Element) => el.getBoundingClientRect()
const undoEl = () => document.querySelector('[title="undo edit title"]')!
const contentsToggle = () =>
  document.querySelector<HTMLElement>(
    '[aria-label="Show sidebar"], [aria-label="Hide sidebar"]'
  )!
const contentsPane = () => document.querySelector("aside")!

async function mountDeepNote(width: number) {
  await atViewportWidth(width)
  const realm = deepNote()
  const mounted = await mountNotebookWorkspace([realm])
  useNoteStore().noteUndo.addEditingToUndoHistory(realm.id, "edit title", "Old")
  await flushPromises()
  return { ...mounted, realm }
}

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
        const context = rect(contextEl())
        expect(pageBarEl().contains(contextEl())).toBe(false)
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
    "shows a long trail in the bar before the commands at %ipx while tools keep their space",
    async (width) => {
      ;({ wrapper } = await mountDeepNote(width))

      await vi.waitFor(() => {
        const context = rect(contextEl())
        const bar = rect(pageBarEl())
        expect(context.top).toBeGreaterThanOrEqual(bar.top)
        expect(context.bottom).toBeLessThanOrEqual(bar.bottom)
        expect(context.right).toBeLessThanOrEqual(
          rect(noteCommandEls()[0]!).left
        )
        expect(toolsFitInBar(width)).toBe(true)
      })
    }
  )

  it("scrolls the narrow trail to a parent folder and opens it by its link", async () => {
    const mounted = await mountDeepNote(320)
    const { router, realm } = mounted
    wrapper = mounted.wrapper
    const folderRealm = makeMe.aFolderRealm
      .folder(parentFolder.id, parentFolder.name)
      .please()
    folderRealm.notebookRealm = realm.notebookRealm
    mockSdkService(NotebookFolderController, "getFolderPage", folderRealm)

    const link = [...contextEl().querySelectorAll("a")].find((a) =>
      a.textContent?.includes(parentFolder.name)
    )!
    link.scrollIntoView({ inline: "center" })
    await page.elementLocator(link).click()
    await flushPromises()

    await vi.waitFor(() => {
      expect(router.currentRoute.value.name).toBe("folderPage")
      expect(router.currentRoute.value.params.folderId).toBe(
        String(parentFolder.id)
      )
      expect(document.querySelector("[data-note-toolbar]")).toBeNull()
      expect(
        document.querySelector(
          '[data-note-sidebar-toolbar] [title="New folder"]'
        )
      ).not.toBeNull()
    })
  })

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

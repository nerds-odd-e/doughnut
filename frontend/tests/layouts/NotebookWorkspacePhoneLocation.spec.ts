import { NotebookFolderController } from "@generated/donut-backend-api/sdk.gen"
import {
  noteMoreOptionsTitles,
  noteToolbarEditTitles,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { noteShowLocation } from "@/routes/noteShowLocation"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { resetPeerSortStorage } from "@tests/notes/sidebar/sidebarTestSupport"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { page, userEvent } from "vitest/browser"
import {
  atViewportWidth,
  chooseFromMore,
  commandsSitInPageBar,
  mountNotebookWorkspace,
  pageBarCommand,
} from "./notebookWorkspaceHeaderTestSupport"
import {
  innerFolder,
  longNotebook,
  notebookLocationControl,
  parentFolder,
  populatedNotebook,
} from "./notebookWorkspaceContextTestSupport"

const rect = (element: Element) => element.getBoundingClientRect()
const tree = () => document.querySelector<HTMLElement>(".sidebar-tree-scroll")!
const toggle = () =>
  document.querySelector<HTMLElement>(
    '[aria-label="Show sidebar"], [aria-label="Hide sidebar"]'
  )!
async function toggleContents() {
  await page.elementLocator(toggle()).click()
  await flushPromises()
}

describe("Notebook workspace phone location", () => {
  let wrapper: VueWrapper
  beforeEach(() => {
    useAssimilationView().dismiss()
    resetPeerSortStorage()
  })
  afterEach(() => {
    wrapper.unmount()
    document.body.innerHTML = ""
  })
  async function mountPhone(width: number) {
    await atViewportWidth(width)
    const { notes, folderListings } = populatedNotebook("Phone note")
    const mounted = await mountNotebookWorkspace(
      notes,
      noteShowLocation(notes[0]!.id),
      folderListings
    )
    wrapper = mounted.wrapper
    return { ...mounted, notes }
  }

  it.each([320, 390])(
    "keeps drawer location fixed over real scrolling rows and follows a full-name ancestor at %ipx",
    async (width) => {
      const { router, notes } = await mountPhone(width)
      expect(
        document.querySelector("main")!.contains(notebookLocationControl())
      ).toBe(true)
      await toggleContents()
      await vi.waitFor(() => {
        expect(tree().scrollHeight).toBeGreaterThan(tree().clientHeight)
        expect(rect(document.querySelector("aside")!).left).toBe(0)
      })
      const location = notebookLocationControl()
      expect(document.querySelector("aside")!.contains(location)).toBe(true)
      expect(rect(location).bottom).toBeLessThanOrEqual(rect(tree()).top)
      const commands = document.querySelector("[data-note-sidebar-toolbar]")!
      expect(rect(commands).bottom).toBeLessThanOrEqual(rect(location).top)
      expect(
        commands.querySelector(`[title="${noteMoreOptionsTitles.new}"]`)
      ).not.toBeNull()
      expect(commands.querySelector('[title="New folder"]')).not.toBeNull()
      expect(commands.querySelector('[title="Sort sidebar"]')).not.toBeNull()
      const top = rect(location).top
      const main = document.querySelector("main")!
      const mainScroll = main.scrollTop
      tree().scrollTop = tree().scrollHeight
      await new Promise(requestAnimationFrame)
      expect(tree().scrollTop).toBeGreaterThan(0)
      expect(main.scrollTop).toBe(mainScroll)
      expect(rect(location).top).toBe(top)
      await page
        .elementLocator(
          commands.querySelector("[data-note-sidebar-sort] summary")!
        )
        .click()
      await page
        .elementLocator(document.querySelector('[title="Title (Z–A)"]')!)
        .click()
      await vi.waitFor(() => {
        const titles = [
          ...tree().querySelectorAll(
            '[role="treeitem"][aria-label^="Phone note"]'
          ),
        ].map((row) => row.getAttribute("aria-label"))
        expect(titles[0]).toBe("Phone note 40")
        expect(rect(location).top).toBe(top)
      })
      await page.screenshot({
        path: `../../vitest-test-results/sidebar-location-${width}-phone-drawer.png`,
      })

      mockSdkService(NotebookFolderController, "getFolderPage", {
        ...makeMe.aFolderRealm
          .folder(parentFolder.id, parentFolder.name)
          .please(),
        notebookRealm: notes[0]!.notebookRealm,
      })
      location.focus()
      await userEvent.keyboard("{Enter}")
      await flushPromises()
      const link = [...document.querySelectorAll("dialog a")].find((a) =>
        a.textContent?.includes(parentFolder.name)
      )!
      expect(rect(link).height).toBeGreaterThanOrEqual(44)
      expect(link.scrollWidth).toBeLessThanOrEqual(link.clientWidth)
      expect(document.querySelector("dialog")!.textContent).toContain(
        longNotebook
      )
      expect(document.querySelector("dialog")!.textContent).toContain(
        innerFolder.name
      )
      await page.elementLocator(link).click()
      await flushPromises()
      expect(router.currentRoute.value).toMatchObject({
        name: "folderPage",
        params: { folderId: String(parentFolder.id) },
      })
      expect(
        document.querySelector("main")!.contains(notebookLocationControl())
      ).toBe(true)
      await toggleContents()
      expect(
        document.querySelector("aside")!.contains(notebookLocationControl())
      ).toBe(true)
      await toggleContents()
      expect(
        document.querySelector("main")!.contains(notebookLocationControl())
      ).toBe(true)
    }
  )

  it("keeps an active editor through drawer toggles and the 767/768 breakpoint, then closes the drawer on note navigation", async () => {
    const { router, notes } = await mountPhone(320)
    await vi.waitFor(() =>
      expect(pageBarCommand(noteMoreOptionsTitles.overflowMenu)).not.toBeNull()
    )
    await chooseFromMore(noteToolbarEditTitles.markdown)
    const editor = document.querySelector<HTMLTextAreaElement>(
      "#main-note-content textarea"
    )!
    editor.value = "A pending orchard thought"
    editor.dispatchEvent(new Event("input", { bubbles: true }))
    await toggleContents()
    await toggleContents()
    await toggleContents()
    for (const width of [767, 768, 767]) {
      await page.viewport(width, 700)
      await vi.waitFor(() => {
        expect(
          document.querySelector(".sidebar-drawer-backdrop") !== null
        ).toBe(width < 768)
        expect(commandsSitInPageBar(width)).toBe(true)
        expect(
          document.querySelector<HTMLTextAreaElement>(
            "#main-note-content textarea"
          )!.value
        ).toBe("A pending orchard thought")
        expect(
          document.querySelector("aside")!.contains(notebookLocationControl())
        ).toBe(true)
        expect(rect(notebookLocationControl()).bottom).toBeLessThanOrEqual(
          rect(tree()).top
        )
      })
    }
    await router.push(noteShowLocation(notes[1]!.id))
    await flushPromises()
    expect(toggle().getAttribute("aria-label")).toBe("Show sidebar")
    expect(
      document.querySelector("main")!.contains(notebookLocationControl())
    ).toBe(true)
  })
})

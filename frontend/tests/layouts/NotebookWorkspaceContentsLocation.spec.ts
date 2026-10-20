import {
  NoteController,
  NotebookFolderController,
} from "@generated/donut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/donut-backend-api"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { useAssimilationView } from "@/composables/useAssimilationView"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  testFolderStub,
} from "@tests/helpers"
import { resetPeerSortStorage } from "@tests/notes/sidebar/sidebarTestSupport"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  mountNotebookWorkspace,
} from "./notebookWorkspaceHeaderTestSupport"
import {
  deepNote,
  innerFolder,
  parentFolder,
  notebookLocationControl,
} from "./notebookWorkspaceContextTestSupport"

const rect = (element: Element) => element.getBoundingClientRect()
const tree = () => document.querySelector<HTMLElement>(".sidebar-tree-scroll")!
const row = (title: string) =>
  document.querySelector<HTMLElement>(
    `[role="treeitem"][aria-label="${title}"]`
  )!
const toggle = () =>
  document.querySelector<HTMLElement>(
    '[aria-label="Show sidebar"], [aria-label="Hide sidebar"]'
  )!
async function settledScroll() {
  let before: number
  do {
    before = tree().scrollTop
    await new Promise(requestAnimationFrame)
    await new Promise(requestAnimationFrame)
  } while (tree().scrollTop !== before)
  return before
}
function expectWholeRow(title: string) {
  expect(rect(row(title)).top).toBeGreaterThanOrEqual(rect(tree()).top)
  expect(rect(row(title)).bottom).toBeLessThanOrEqual(rect(tree()).bottom)
}

describe("Notebook workspace contents location", () => {
  let wrapper: VueWrapper
  beforeEach(() => {
    useAssimilationView().dismiss()
    resetPeerSortStorage()
  })
  afterEach(() => {
    wrapper.unmount()
    document.body.innerHTML = ""
  })
  async function mountPopulated(width: number) {
    await atViewportWidth(width)
    const notebook = deepNote().notebookRealm.notebook
    const notes = Array.from({ length: 40 }, (_, i) =>
      makeMe.aNoteRealm
        .title(`Note ${String(i + 1).padStart(2, "0")}`)
        .inNotebook(notebook.id, notebook.name)
        .ancestorFolders([parentFolder, innerFolder])
        .please()
    )
    const mounted = await mountNotebookWorkspace(
      notes,
      noteShowLocation(notes[29]!.id),
      {
        undefined: {
          noteTopologies: [],
          folders: [testFolderStub(parentFolder.id, parentFolder.name)],
        },
        [parentFolder.id]: {
          noteTopologies: [],
          folders: [testFolderStub(innerFolder.id, innerFolder.name)],
        },
        [innerFolder.id]: {
          noteTopologies: notes.map((note) => note.note.noteTopology),
          folders: [],
        },
      }
    )
    wrapper = mounted.wrapper
    await vi.waitFor(async () => {
      await settledScroll()
      expectWholeRow("Note 30")
    })
    return { ...mounted, notes }
  }

  it.each([768, 1280])(
    "keeps location fixed over populated scrolling contents and reveals navigation/sort at %ipx",
    async (width) => {
      const { router, notes } = await mountPopulated(width)
      expect(tree().scrollHeight).toBeGreaterThan(tree().clientHeight)
      const location = rect(notebookLocationControl())
      tree().scrollTop = 0
      await settledScroll()
      expect(rect(notebookLocationControl()).top).toBe(location.top)
      expect(rect(notebookLocationControl()).bottom).toBeLessThanOrEqual(
        rect(tree()).top
      )
      await page.screenshot({
        path: `../../vitest-test-results/sidebar-location-${width}-tree.png`,
      })

      await router.push(noteShowLocation(notes[30]!.id))
      await flushPromises()
      await vi.waitFor(async () => {
        await settledScroll()
        expectWholeRow("Note 31")
      })
      const nextRow = rect(row("Note 32"))
      tree().scrollTop +=
        nextRow.bottom - rect(tree()).bottom - nextRow.height / 2
      const before = await settledScroll()
      expect(rect(row("Note 32")).bottom).toBeGreaterThan(rect(tree()).bottom)
      await router.push(noteShowLocation(notes[31]!.id))
      await flushPromises()
      await vi.waitFor(async () => {
        await settledScroll()
        expectWholeRow("Note 32")
      })
      expect(Math.abs(tree().scrollTop - before)).toBeLessThan(nextRow.height)

      await page
        .elementLocator(
          document.querySelector("[data-note-sidebar-sort] summary")!
        )
        .click()
      await page
        .elementLocator(document.querySelector('[title="Title (Z–A)"]')!)
        .click()
      await flushPromises()
      await vi.waitFor(async () => {
        await settledScroll()
        expectWholeRow("Note 32")
      })
      expect(rect(row("Note 33")).top).toBeLessThan(rect(row("Note 32")).top)
      expect(rect(notebookLocationControl()).top).toBe(location.top)
      await page.screenshot({
        path: `../../vitest-test-results/sidebar-location-${width}-active-tree.png`,
      })
    }
  )

  it("navigates ancestors from the main fallback and restores location to docked contents", async () => {
    const { router, notes } = await mountPopulated(768)
    await page.elementLocator(toggle()).click()
    await flushPromises()
    const location = notebookLocationControl()
    expect(document.querySelector("main")!.contains(location)).toBe(true)
    expect(rect(location).bottom).toBeLessThanOrEqual(
      rect(document.querySelector("#main-note-content")!).top
    )
    mockSdkService(NotebookFolderController, "getFolderPage", {
      ...makeMe.aFolderRealm
        .folder(parentFolder.id, parentFolder.name)
        .please(),
      notebookRealm: notes[0]!.notebookRealm,
    })
    await page.elementLocator(location).click()
    await page
      .elementLocator(
        [...document.querySelectorAll("dialog a")].find((link) =>
          link.textContent?.includes(parentFolder.name)
        )!
      )
      .click()
    await flushPromises()
    expect(router.currentRoute.value.name).toBe("folderPage")
    expect(router.currentRoute.value.params.folderId).toBe(
      String(parentFolder.id)
    )
    await page.elementLocator(toggle()).click()
    await flushPromises()
    expect(
      document.querySelector("aside")!.contains(notebookLocationControl())
    ).toBe(true)
    expect(rect(notebookLocationControl()).bottom).toBeLessThanOrEqual(
      rect(tree()).top
    )
  })

  it("retains location and active contents while a same-notebook route awaits its note", async () => {
    const { router, notes } = await mountPopulated(768)
    const pending = makeMe.aNoteRealm
      .title("Pending orchard note")
      .inNotebook(
        notes[0]!.notebookRealm.notebook.id,
        notes[0]!.notebookRealm.notebook.name
      )
      .ancestorFolders([parentFolder])
      .please()
    let resolve!: (realm: NoteRealm) => void
    mockSdkServiceWithImplementation(
      NoteController,
      "showNote",
      () =>
        new Promise<NoteRealm>((done) => {
          resolve = done
        })
    )
    const text = notebookLocationControl().textContent
    const position = rect(notebookLocationControl()).top
    await router.push(noteShowLocation(pending.id))
    await flushPromises()
    expect(notebookLocationControl().textContent).toBe(text)
    expect(rect(notebookLocationControl()).top).toBe(position)
    expect(row("Note 30").getAttribute("aria-selected")).toBe("true")
    resolve(pending)
    await flushPromises()
    await vi.waitFor(() =>
      expect(notebookLocationControl().textContent).toContain(parentFolder.name)
    )
    expect(
      notebookLocationControl().querySelector(".text-xs")!.textContent
    ).toBe(parentFolder.name)
  })
})

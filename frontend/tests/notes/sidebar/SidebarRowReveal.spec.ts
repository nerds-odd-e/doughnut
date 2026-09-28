import { invalidateSidebarListingCache } from "@/components/notes/sidebarFolderListingCache"
import { resetNoteStore, useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { testFolderStub } from "@tests/helpers"
import type { NoteRealm } from "@generated/donut-backend-api"
import type { VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  findSidebarItem,
  mockShowNoteForRealms,
  mountSidebarSignedIn,
  resetPeerSortStorage,
  stubNotebookFolderListings,
  teardownSidebarComponentTest,
} from "./sidebarTestSupport"

const NOTES_FOLDER_ID = 78001

function fortyNotesInOneFolder() {
  const folder = testFolderStub(NOTES_FOLDER_ID, "Notes")
  const notes = Array.from({ length: 40 }, (_, i) =>
    makeMe.aNoteRealm
      .title(`Note ${String(i + 1).padStart(2, "0")}`)
      .ancestorFolders([{ id: folder.id, name: folder.name }])
      .please()
  )
  return { folder, notes }
}

function nextAnimationFrame() {
  return new Promise((resolve) => requestAnimationFrame(resolve))
}

describe("Sidebar row reveal", () => {
  let wrapper: VueWrapper<unknown> | undefined
  let treeHeightStyle: HTMLStyleElement

  beforeEach(() => {
    invalidateSidebarListingCache()
    resetPeerSortStorage()
    resetNoteStore()
    treeHeightStyle = document.createElement("style")
    treeHeightStyle.textContent = ".sidebar-tree-scroll{height:300px;flex:none}"
    document.head.appendChild(treeHeightStyle)
  })

  afterEach(() => {
    teardownSidebarComponentTest(wrapper)
    treeHeightStyle.remove()
  })

  function tree() {
    return document.querySelector(".sidebar-tree-scroll")!
  }

  function rowRect(title: string) {
    return findSidebarItem(wrapper!, title)!.element.getBoundingClientRect()
  }

  function isWholeRowVisible(title: string) {
    const row = rowRect(title)
    const treeRect = tree().getBoundingClientRect()
    return row.top >= treeRect.top && row.bottom <= treeRect.bottom
  }

  async function settledScrollTop() {
    let scrollTopBefore: number
    do {
      scrollTopBefore = tree().scrollTop
      await nextAnimationFrame()
      await nextAnimationFrame()
    } while (tree().scrollTop !== scrollTopBefore)
    return scrollTopBefore
  }

  async function openNoteThirtyInFortyNoteFolder() {
    const { folder, notes } = fortyNotesInOneFolder()
    const noteStore = useNoteStore()
    for (const n of notes) noteStore.refOfNoteRealm(n.id).value = n
    stubNotebookFolderListings({
      [String(undefined)]: { noteTopologies: [], folders: [folder] },
      [String(NOTES_FOLDER_ID)]: {
        noteTopologies: notes.map((n) => ({ ...n.note.noteTopology })),
        folders: [],
      },
    })
    mockShowNoteForRealms(notes)
    const active = notes[29]!
    wrapper = mountSidebarSignedIn(
      helper,
      active,
      active.notebookRealm.notebook.id
    )
    await vi.waitFor(async () => {
      await settledScrollTop()
      expect(isWholeRowVisible("Note 30")).toBe(true)
    })
    return notes
  }

  async function scrollTreeBy(offset: number) {
    tree().scrollTop += offset
    return settledScrollTop()
  }

  async function activate(note: NoteRealm) {
    await wrapper!.setProps({
      activeNoteRealm: note,
      breadcrumbFolders: note.ancestorFolders ?? [],
    })
    return settledScrollTop()
  }

  it("shows the whole opened note row with nothing drawn over it", async () => {
    await openNoteThirtyInFortyNoteFolder()
    const row = findSidebarItem(wrapper!, "Note 30")!.element
    const rect = row.getBoundingClientRect()
    const hit = document.elementFromPoint(rect.left + 2, rect.top + 2)
    expect(row.contains(hit)).toBe(true)
  })

  it("scrolls a row cut off at the bottom just enough to show all of it", async () => {
    const notes = await openNoteThirtyInFortyNoteFolder()
    const row = rowRect("Note 31")
    const scrollTopBefore = await scrollTreeBy(
      row.bottom - tree().getBoundingClientRect().bottom - row.height / 2
    )
    expect(isWholeRowVisible("Note 31")).toBe(false)

    const scrollTopAfter = await activate(notes[30]!)

    expect(isWholeRowVisible("Note 31")).toBe(true)
    expect(Math.abs(scrollTopAfter - scrollTopBefore)).toBeLessThan(row.height)
  })

  it("scrolls a row cut off at the top to show all of it", async () => {
    const notes = await openNoteThirtyInFortyNoteFolder()
    const row = rowRect("Note 31")
    await scrollTreeBy(
      row.top - tree().getBoundingClientRect().top + row.height / 2
    )
    expect(isWholeRowVisible("Note 31")).toBe(false)

    await activate(notes[30]!)

    expect(isWholeRowVisible("Note 31")).toBe(true)
  })

  it("does not scroll when the newly active row is already fully visible", async () => {
    const notes = await openNoteThirtyInFortyNoteFolder()
    const row = rowRect("Note 25")
    const scrollTopBefore = await scrollTreeBy(
      row.top - tree().getBoundingClientRect().top - 2 * row.height
    )
    expect(isWholeRowVisible("Note 25")).toBe(true)

    expect(await activate(notes[24]!)).toBe(scrollTopBefore)
  })
})

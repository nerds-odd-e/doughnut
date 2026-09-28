import { invalidateSidebarListingCache } from "@/components/notes/sidebarFolderListingCache"
import { resetNoteStore, useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { testFolderStub } from "@tests/helpers"
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

  it("shows the whole opened note row with nothing drawn over it", async () => {
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

    const tree = document.querySelector(".sidebar-tree-scroll")!
    await vi.waitFor(async () => {
      const scrollTopBefore = tree.scrollTop
      await nextAnimationFrame()
      await nextAnimationFrame()
      expect(tree.scrollTop).toBe(scrollTopBefore)
      const row = findSidebarItem(wrapper!, "Note 30")!.element
      const rowRect = row.getBoundingClientRect()
      const treeRect = tree.getBoundingClientRect()
      expect(rowRect.top).toBeGreaterThanOrEqual(treeRect.top)
      expect(rowRect.bottom).toBeLessThanOrEqual(treeRect.bottom)
    })
    const row = findSidebarItem(wrapper, "Note 30")!.element
    const rowRect = row.getBoundingClientRect()
    const hit = document.elementFromPoint(rowRect.left + 2, rowRect.top + 2)
    expect(row.contains(hit)).toBe(true)
  })
})

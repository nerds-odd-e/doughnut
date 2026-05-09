import {
  NoteController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import createNoteStorage from "@/store/createNoteStorage"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  FOLDER_FIRST_GEN_CHILDREN_ID,
  FOLDER_TOP_NOTE_CHILDREN_ID,
  countFolderListingCallsForParent,
  findSidebarItem,
  folderListingForQueryParent,
  isBefore,
  mountSidebar,
  prepareSidebarDefaultMountContext,
  teardownSidebarComponentTest,
} from "./sidebarTestSupport"

describe("Sidebar gradual ancestor population", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: import("@vue/test-utils").VueWrapper<any>
  const storageAccessor = useStorageAccessor()
  const fixtures = sidebarDefaultTreeFixtures

  beforeEach(() => {
    prepareSidebarDefaultMountContext({
      storageAccessor,
      fixtures,
      vi,
    })
  })

  beforeEach(() => {
    storageAccessor.value = createNoteStorage()
  })

  afterEach(() => {
    teardownSidebarComponentTest(wrapper)
  })

  it("loads ancestor branches for a deep note through folder listings without showNote", async () => {
    const listingSpy = mockSdkServiceWithImplementation(
      NotebookController,
      "listNotebookFolderListing",
      (options) =>
        folderListingForQueryParent(options, fixtures.defaultTreeFolderListings)
    )
    const showNoteSpy = mockSdkServiceWithImplementation(
      NoteController,
      "showNote",
      () => {
        throw new Error("Sidebar must not use showNote for structural branches")
      }
    )

    wrapper = mountSidebar(helper, fixtures, fixtures.secondGeneration)
    await flushPromises()

    expect(countFolderListingCallsForParent(listingSpy, undefined)).toBe(1)
    expect(showNoteSpy).not.toHaveBeenCalled()
    const listedParentIds = listingSpy.mock.calls
      .map((call) => (call[0] as { query?: { parent?: number } }).query?.parent)
      .filter((id): id is number => id !== undefined)
    expect(listedParentIds).toContain(FOLDER_TOP_NOTE_CHILDREN_ID)
    expect(listedParentIds).toContain(FOLDER_FIRST_GEN_CHILDREN_ID)
    expect(listedParentIds).not.toContain(fixtures.firstGenerationSibling.id)

    await vi.waitUntil(() =>
      findSidebarItem(
        wrapper,
        fixtures.secondGeneration.note.noteTopology.title
      )?.exists()
    )
    expect(
      findSidebarItem(
        wrapper,
        fixtures.topNoteRealm.note.noteTopology.title
      )?.exists()
    ).toBe(true)
    expect(
      findSidebarItem(
        wrapper,
        fixtures.firstGeneration.note.noteTopology.title
      )?.exists()
    ).toBe(true)
    expect(
      findSidebarItem(
        wrapper,
        fixtures.firstGenerationSibling.note.noteTopology.title
      )?.exists()
    ).toBe(true)

    const secondGenEl = findSidebarItem(
      wrapper,
      fixtures.secondGeneration.note.noteTopology.title
    )!.element
    const siblingEl = findSidebarItem(
      wrapper,
      fixtures.firstGenerationSibling.note.noteTopology.title
    )!.element
    expect(isBefore(secondGenEl, siblingEl)).toBe(true)
  })
})

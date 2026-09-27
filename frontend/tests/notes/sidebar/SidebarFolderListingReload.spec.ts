import { NotebookFolderController } from "@generated/donut-backend-api/sdk.gen"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import { useNoteStore } from "@/store/noteStore"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  countFolderListingCallsForParent,
  findSidebarItem,
  folderListingForQueryParent,
  mountSidebar,
  prepareSidebarDefaultMountContext,
  teardownSidebarComponentTest,
  withTrackingGlobalApiClient,
} from "./sidebarTestSupport"

describe("Sidebar folder listing reload", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: import("@vue/test-utils").VueWrapper<any>
  const noteStore = useNoteStore()
  const fixtures = sidebarDefaultTreeFixtures

  beforeEach(() => {
    prepareSidebarDefaultMountContext({
      noteStore,
      fixtures,
      vi,
    })
  })

  afterEach(() => {
    teardownSidebarComponentTest(wrapper)
  })

  it("does not trigger the global loading indicator for structural folder listing fetches", async () => {
    await withTrackingGlobalApiClient(async (apiStatus) => {
      mockSdkServiceWithImplementation(
        NotebookFolderController,
        "listNotebookFolderListing",
        (options) =>
          folderListingForQueryParent(
            options,
            fixtures.defaultTreeFolderListings
          )
      )
      wrapper = mountSidebar(helper, fixtures.firstGeneration)
      await flushPromises()
      expect(apiStatus.states).toHaveLength(0)
    })
  })

  it("does not reload notebook root notes when active note changes within the same notebook", async () => {
    const listingSpy = mockSdkServiceWithImplementation(
      NotebookFolderController,
      "listNotebookFolderListing",
      (options) =>
        folderListingForQueryParent(options, fixtures.defaultTreeFolderListings)
    )
    wrapper = mountSidebar(helper, fixtures.firstGeneration)
    await flushPromises()
    const rootRequestCount = () =>
      countFolderListingCallsForParent(listingSpy, undefined)
    expect(rootRequestCount()).toBe(1)

    await wrapper.setProps({
      activeNoteRealm: fixtures.secondGeneration,
      notebookId: fixtures.firstGeneration.notebookRealm.notebook.id,
    })
    await flushPromises()
    expect(rootRequestCount()).toBe(1)
    expect(
      findSidebarItem(
        wrapper,
        fixtures.topNoteRealm.note.noteTopology.title
      )?.exists()
    ).toBe(true)
  })

  it("keeps earlier rows and shows an inline error when listing refresh fails", async () => {
    await withTrackingGlobalApiClient(async (apiStatus) => {
      let shouldFail = false
      mockSdkServiceWithImplementation(
        NotebookFolderController,
        "listNotebookFolderListing",
        (options) => {
          if (shouldFail) throw new Error("Network error")
          return folderListingForQueryParent(
            options,
            fixtures.defaultTreeFolderListings
          )
        }
      )
      wrapper = mountSidebar(helper, fixtures.firstGeneration)
      await flushPromises()

      expect(
        findSidebarItem(
          wrapper,
          fixtures.topNoteRealm.note.noteTopology.title
        )?.exists()
      ).toBe(true)
      expect(wrapper.text()).not.toContain(
        "Could not load this folder's contents."
      )

      shouldFail = true
      refreshSidebarStructuralListings()
      await flushPromises()

      expect(
        findSidebarItem(
          wrapper,
          fixtures.topNoteRealm.note.noteTopology.title
        )?.exists()
      ).toBe(true)
      expect(wrapper.text()).toContain("Could not load this folder's contents.")
      expect(apiStatus.states).toHaveLength(0)
    })
  })

  it("shows an inline message when the first load of a folder listing fails", async () => {
    mockSdkServiceWithImplementation(
      NotebookFolderController,
      "listNotebookFolderListing",
      () => {
        throw new Error("Network error")
      }
    )
    wrapper = mountSidebar(helper, fixtures.firstGeneration)
    await flushPromises()

    expect(wrapper.text()).toContain("Could not load this folder's contents.")
  })
})

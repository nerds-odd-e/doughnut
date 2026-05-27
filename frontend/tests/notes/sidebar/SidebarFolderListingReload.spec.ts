import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
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
  const storageAccessor = useStorageAccessor()
  const fixtures = sidebarDefaultTreeFixtures

  beforeEach(() => {
    prepareSidebarDefaultMountContext({
      storageAccessor,
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
        NotebookController,
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
      NotebookController,
      "listNotebookFolderListing",
      (options) =>
        folderListingForQueryParent(options, fixtures.defaultTreeFolderListings)
    )
    wrapper = mountSidebar(helper, fixtures.firstGeneration)
    await flushPromises()
    const rootRequestCount = () =>
      countFolderListingCallsForParent(listingSpy, undefined)
    expect(rootRequestCount()).toBe(1)
    expect(
      findSidebarItem(
        wrapper,
        fixtures.topNoteRealm.note.noteTopology.title
      )?.exists()
    ).toBe(true)

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
})

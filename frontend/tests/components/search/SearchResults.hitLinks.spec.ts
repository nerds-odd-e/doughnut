import type { RelationshipLiteralSearchHit } from "@generated/donut-backend-api"
import {
  NoteController,
  SearchController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { describe, it, expect, vi } from "vitest"
import {
  mountSearchResults,
  notebookIdFromStubbedLinkTo,
  waitForDebounce,
} from "./searchResultsTestSupport"

describe("SearchResults folder and notebook hits", () => {
  it("dropdown shows folder hit as router-link to folder page", async () => {
    vi.useFakeTimers()
    const folderHit: RelationshipLiteralSearchHit = {
      hitKind: "FOLDER",
      folderId: 42,
      folderName: "Specs Archive",
      notebookId: 1,
      notebookName: "My NB",
      distance: 0.9,
    }
    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTargetWithin",
      vi.fn().mockResolvedValue([folderHit])
    )
    mockSdkServiceWithImplementation(
      SearchController,
      "semanticSearchWithin",
      vi.fn().mockResolvedValue([])
    )
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(NoteController, "getRecentNotes", [])

    const wrapper = mountSearchResults({
      inputSearchKey: "spec",
      noteId: 1,
      isDropdown: true,
    })
    await waitForDebounce()

    expect(wrapper.text()).toContain("Specs Archive")
    expect(wrapper.text()).toContain("My NB")
    const folderRow = wrapper.find(".folder-search-hit")
    expect(folderRow.exists()).toBe(true)
    const link = folderRow.find(".router-link")
    expect(link.exists()).toBe(true)
    const to = JSON.parse(link.attributes("to") ?? "{}") as {
      name?: string
      params?: { notebookId?: number; folderId?: number }
    }
    expect(to.name).toBe("folderPage")
    expect(to.params?.notebookId).toBe(1)
    expect(to.params?.folderId).toBe(42)
    vi.useRealTimers()
  })

  it("dropdown shows notebook hit as router-link to notebook page", async () => {
    vi.useFakeTimers()
    const notebookHit: RelationshipLiteralSearchHit = {
      hitKind: "NOTEBOOK",
      notebookId: 99,
      notebookName: "Field Guide",
      distance: 0.0,
    }
    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTargetWithin",
      vi.fn().mockResolvedValue([notebookHit])
    )
    mockSdkServiceWithImplementation(
      SearchController,
      "semanticSearchWithin",
      vi.fn().mockResolvedValue([])
    )
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(NoteController, "getRecentNotes", [])

    const wrapper = mountSearchResults({
      inputSearchKey: "field",
      noteId: 1,
      isDropdown: true,
    })
    await waitForDebounce()

    expect(wrapper.text()).toContain("Field Guide")
    const links = wrapper.findAll(".router-link")
    const notebookIds = links
      .map((a) => notebookIdFromStubbedLinkTo(a.attributes("to") ?? "{}"))
      .filter((id): id is number => id != null)
    expect(notebookIds).toContain(99)
    vi.useRealTimers()
  })
})

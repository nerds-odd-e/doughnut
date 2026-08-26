import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { nextTick } from "vue"
import { describe, it, expect, vi } from "vitest"
import {
  asLiteralHits,
  mountSearchResults,
  noteIdFromStubbedLinkTo,
  recentNotes,
  searchResult,
  waitForDebounce,
} from "./searchResultsTestSupport"

describe("SearchResults search caching", () => {
  it("triggers second API call when context changes (noteId added)", async () => {
    vi.useFakeTimers()

    const result = asLiteralHits([searchResult(1, "Alpha")])
    const firstSpy = vi.fn().mockResolvedValue(result)
    const withinSpy = vi.fn().mockResolvedValue(result)
    const semanticSpy = vi.fn().mockResolvedValue([])
    const semanticWithinSpy = vi.fn().mockResolvedValue([])

    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTarget",
      firstSpy
    )
    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTargetWithin",
      withinSpy
    )
    mockSdkServiceWithImplementation(
      SearchController,
      "semanticSearch",
      semanticSpy
    )
    mockSdkServiceWithImplementation(
      SearchController,
      "semanticSearchWithin",
      semanticWithinSpy
    )
    mockSdkService(NoteController, "getRecentNotes", [])

    const wrapper = mountSearchResults({
      inputSearchKey: "a",
      isDropdown: true,
    })
    await waitForDebounce()

    await wrapper.setProps({ noteId: 1, inputSearchKey: "a " })
    await waitForDebounce()

    expect(firstSpy).toHaveBeenCalledTimes(1)
    expect(semanticSpy).toHaveBeenCalledTimes(1)
    expect(withinSpy).toHaveBeenCalledTimes(1)
    expect(semanticWithinSpy).toHaveBeenCalledTimes(1)

    vi.useRealTimers()
  })

  it("merges unique results and sorts by ascending distance", async () => {
    vi.useFakeTimers()

    const firstBatch = [searchResult(2, "N2", 0.4), searchResult(1, "N1", 0.2)]
    const secondBatch = [searchResult(1, "N1", 0.1), searchResult(3, "N3", 0.8)]

    const mockTop = vi.fn().mockResolvedValueOnce(asLiteralHits(firstBatch))
    const mockWithin = vi.fn().mockResolvedValueOnce(asLiteralHits(secondBatch))
    const mockSemanticTop = vi.fn().mockResolvedValueOnce([])
    const mockSemanticWithin = vi.fn().mockResolvedValueOnce([])

    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTarget",
      mockTop
    )
    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTargetWithin",
      mockWithin
    )
    mockSdkServiceWithImplementation(
      SearchController,
      "semanticSearch",
      mockSemanticTop
    )
    mockSdkServiceWithImplementation(
      SearchController,
      "semanticSearchWithin",
      mockSemanticWithin
    )
    mockSdkService(NoteController, "getRecentNotes", [])

    const wrapper = mountSearchResults({
      inputSearchKey: "x",
      isDropdown: true,
    })
    await waitForDebounce()

    await wrapper.setProps({ noteId: 1, inputSearchKey: "x " })
    await waitForDebounce()

    const links = wrapper.findAll(".router-link")
    const ids = links.map((a) =>
      noteIdFromStubbedLinkTo(a.attributes("to") ?? "{}")
    )

    expect(ids.filter((x) => x !== undefined)).toEqual([1, 2, 3])
    vi.useRealTimers()
  })

  it("prioritizes same-notebook results when distances are equal", async () => {
    vi.useFakeTimers()

    const currentNotebookId = 10
    const sameNotebookResult = makeMe.aNoteSearchResult
      .id(2)
      .title("Same Notebook Note")
      .notebookId(currentNotebookId)
      .distance(0.5)
      .please()
    const otherNotebookResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Other Notebook Note")
      .notebookId(20)
      .distance(0.5)
      .please()

    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTargetWithin",
      vi
        .fn()
        .mockResolvedValue(
          asLiteralHits([otherNotebookResult, sameNotebookResult])
        )
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
      inputSearchKey: "test",
      noteId: 1,
      notebookId: currentNotebookId,
      isDropdown: true,
    })
    await waitForDebounce()

    const links = wrapper.findAll(".router-link")
    const ids = links.map((a) =>
      noteIdFromStubbedLinkTo(a.attributes("to") ?? "{}")
    )

    expect(ids[0]).toBe(2)
    expect(ids[1]).toBe(1)
    vi.useRealTimers()
  })

  it("keeps previous results visible while waiting for new search", async () => {
    vi.useFakeTimers()

    const firstSearchResults = [searchResult(1, "First Result")]
    const secondSearchDelayed = new Promise<Array<unknown>>((resolve) =>
      setTimeout(() => resolve([]), 2000)
    )

    const searchSpy = mockSdkService(
      SearchController,
      "searchForRelationshipTarget",
      []
    )
    searchSpy.mockResolvedValueOnce(
      wrapSdkResponse(asLiteralHits(firstSearchResults))
    )
    searchSpy.mockReturnValue(
      secondSearchDelayed.then((data) => wrapSdkResponse(data)) as never
    )

    const semanticSpy = mockSdkService(SearchController, "semanticSearch", [])
    semanticSpy.mockResolvedValueOnce(wrapSdkResponse([]))
    semanticSpy.mockReturnValue(
      secondSearchDelayed.then((data) => wrapSdkResponse(data)) as never
    )

    mockSdkService(NoteController, "getRecentNotes", recentNotes)

    const wrapper = mountSearchResults({
      inputSearchKey: "first",
      isDropdown: false,
    })
    await waitForDebounce()

    expect(wrapper.text()).toContain("First Result")
    expect(wrapper.text()).toContain("Search result")

    await wrapper.setProps({ inputSearchKey: "first second" })
    await nextTick()
    vi.advanceTimersByTime(100)

    expect(
      wrapper.find(".searching-indicator .daisy-loading-spinner").exists()
    ).toBe(true)
    expect(wrapper.text()).toContain("First Result")
    expect(wrapper.text()).not.toContain("Recent Note 1")

    vi.useRealTimers()
  })
})

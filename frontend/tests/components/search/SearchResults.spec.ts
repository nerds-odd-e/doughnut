import type { RelationshipLiteralSearchHit } from "@generated/donut-backend-api"
import {
  NoteController,
  SearchController,
} from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { nextTick } from "vue"
import { describe, it, expect, vi } from "vitest"
import {
  asLiteralHits,
  mountSearchResults,
  notebookIdFromStubbedLinkTo,
  noteIdFromStubbedLinkTo,
  recentNotes,
  searchResult,
  setupDelayedSearchMocks,
  setupSearchResultsTests,
  waitForDebounce,
} from "./searchResultsTestSupport"

setupSearchResultsTests()

function mountDropdownWithinNoteFinding(
  inputSearchKey: string,
  hit: RelationshipLiteralSearchHit
) {
  mockSdkService(SearchController, "searchForRelationshipTargetWithin", [hit])
  return mountSearchResults({ inputSearchKey, noteId: 1, isDropdown: true })
}

describe("SearchResults.vue", () => {
  it("shows a loading indicator before results arrive", async () => {
    setupDelayedSearchMocks()

    const wrapper = mountSearchResults({
      inputSearchKey: "q",
      isDropdown: true,
    })

    await nextTick()
    vi.advanceTimersByTime(100)

    expect(
      wrapper.find(".searching-indicator .daisy-loading-spinner").exists()
    ).toBe(true)
  })

  it.each([
    {
      when: "a shorter empty phrase is contained, without semantic search",
      semanticSearchEnabled: false,
      nextKey: "abc",
      literalCalls: 1,
      semanticCalls: 0,
    },
    {
      when: "semantic search is enabled",
      semanticSearchEnabled: true,
      nextKey: "abc",
      literalCalls: 2,
      semanticCalls: 2,
    },
    {
      when: "the empty phrase is not contained",
      semanticSearchEnabled: false,
      nextKey: "xy",
      literalCalls: 2,
      semanticCalls: 0,
    },
  ])(
    "searches literal $literalCalls and semantic $semanticCalls times when $when",
    async ({ semanticSearchEnabled, nextKey, literalCalls, semanticCalls }) => {
      const literalSpy = mockSdkService(
        SearchController,
        "searchForRelationshipTarget",
        []
      )
      const semanticSpy = mockSdkService(SearchController, "semanticSearch", [])

      const wrapper = mountSearchResults({
        inputSearchKey: "ab",
        isDropdown: true,
        semanticSearchEnabled,
      })
      await waitForDebounce()
      expect(literalSpy).toHaveBeenCalledTimes(1)

      await wrapper.setProps({ inputSearchKey: nextKey })
      await waitForDebounce()

      expect(literalSpy).toHaveBeenCalledTimes(literalCalls)
      expect(semanticSpy).toHaveBeenCalledTimes(semanticCalls)
      expect(wrapper.text()).toContain("No matching notes found.")
    }
  )

  it("dropdown shows folder hit as router-link to folder page", async () => {
    const wrapper = mountDropdownWithinNoteFinding("spec", {
      hitKind: "FOLDER",
      folderId: 42,
      folderName: "Specs Archive",
      notebookId: 1,
      notebookName: "My NB",
      distance: 0.9,
    })
    await waitForDebounce()

    expect(wrapper.text()).toContain("Specs Archive")
    expect(wrapper.text()).toContain("My NB")
    const link = wrapper.find(".folder-search-hit .router-link")
    expect(link.exists()).toBe(true)
    const to = JSON.parse(link.attributes("to") ?? "{}") as {
      name?: string
      params?: { notebookId?: number; folderId?: number }
    }
    expect(to.name).toBe("folderPage")
    expect(to.params?.notebookId).toBe(1)
    expect(to.params?.folderId).toBe(42)
  })

  it("dropdown shows notebook hit as router-link to notebook page", async () => {
    const wrapper = mountDropdownWithinNoteFinding("field", {
      hitKind: "NOTEBOOK",
      notebookId: 99,
      notebookName: "Field Guide",
      distance: 0.0,
    })
    await waitForDebounce()

    expect(wrapper.text()).toContain("Field Guide")
    const notebookIds = wrapper
      .findAll(".router-link")
      .map((a) => notebookIdFromStubbedLinkTo(a.attributes("to") ?? "{}"))
    expect(notebookIds).toContain(99)
  })
})

const linkedNoteIds = (wrapper: ReturnType<typeof mountSearchResults>) =>
  wrapper
    .findAll(".router-link")
    .map((a) => noteIdFromStubbedLinkTo(a.attributes("to") ?? "{}"))
    .filter((id) => id !== undefined)

describe("SearchResults search caching", () => {
  it("searches again within the note context and merges unique results by ascending distance", async () => {
    const topSpy = mockSdkService(
      SearchController,
      "searchForRelationshipTarget",
      asLiteralHits([searchResult(2, "N2", 0.4), searchResult(1, "N1", 0.2)])
    )
    const withinSpy = mockSdkService(
      SearchController,
      "searchForRelationshipTargetWithin",
      asLiteralHits([searchResult(1, "N1", 0.1), searchResult(3, "N3", 0.8)])
    )
    const semanticSpy = mockSdkService(SearchController, "semanticSearch", [])
    const semanticWithinSpy = mockSdkService(
      SearchController,
      "semanticSearchWithin",
      []
    )

    const wrapper = mountSearchResults({
      inputSearchKey: "x",
      isDropdown: true,
    })
    await waitForDebounce()

    await wrapper.setProps({ noteId: 1, inputSearchKey: "x " })
    await waitForDebounce()

    expect(topSpy).toHaveBeenCalledTimes(1)
    expect(semanticSpy).toHaveBeenCalledTimes(1)
    expect(withinSpy).toHaveBeenCalledTimes(1)
    expect(semanticWithinSpy).toHaveBeenCalledTimes(1)
    expect(linkedNoteIds(wrapper)).toEqual([1, 2, 3])
  })

  it("prioritizes same-notebook results when distances are equal", async () => {
    const currentNotebookId = 10
    const resultIn = (id: number, notebookId: number) =>
      makeMe.aNoteSearchResult
        .id(id)
        .title(`Note ${id}`)
        .notebookId(notebookId)
        .distance(0.5)
        .please()
    mockSdkService(
      SearchController,
      "searchForRelationshipTargetWithin",
      asLiteralHits([resultIn(1, 20), resultIn(2, currentNotebookId)])
    )

    const wrapper = mountSearchResults({
      inputSearchKey: "test",
      noteId: 1,
      notebookId: currentNotebookId,
      isDropdown: true,
    })
    await waitForDebounce()

    expect(linkedNoteIds(wrapper).slice(0, 2)).toEqual([2, 1])
  })

  it("keeps previous results visible while waiting for new search", async () => {
    const secondSearchDelayed = new Promise<Array<unknown>>((resolve) =>
      setTimeout(() => resolve([]), 2000)
    ).then((data) => wrapSdkResponse(data)) as never

    mockSdkService(SearchController, "searchForRelationshipTarget", [])
      .mockResolvedValueOnce(
        wrapSdkResponse(asLiteralHits([searchResult(1, "First Result")]))
      )
      .mockReturnValue(secondSearchDelayed)
    mockSdkService(SearchController, "semanticSearch", [])
      .mockResolvedValueOnce(wrapSdkResponse([]))
      .mockReturnValue(secondSearchDelayed)
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
  })
})

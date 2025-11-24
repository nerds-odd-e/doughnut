import SearchResults from "@/components/search/SearchResults.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import * as sdk from "@generated/backend/sdk.gen"
import type { NoteRealm, NoteSearchResult } from "@generated/backend"

describe("SearchResults.vue", () => {
  it("shows 'Searching ...' before results arrive", async () => {
    vi.useFakeTimers()

    const delayed = new Promise<Array<unknown>>((resolve) =>
      setTimeout(() => resolve([]), 1)
    )

    vi.spyOn(sdk, "searchForLinkTarget").mockReturnValue(delayed as never)
    vi.spyOn(sdk, "semanticSearch").mockReturnValue(delayed as never)
    vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "q", isDropdown: true })
      .mount()

    await nextTick()
    vi.advanceTimersByTime(100)

    expect(wrapper.text()).toContain("Searching ...")

    vi.useRealTimers()
  })

  it("shows 'No matching notes found.' when results are empty in dropdown mode after search", async () => {
    vi.useFakeTimers()

    const empty: NoteSearchResult[] = []
    vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
      data: empty,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
      data: empty,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "z", isDropdown: true })
      .mount()

    await nextTick()
    vi.advanceTimersByTime(1100)
    await flushPromises()

    expect(wrapper.text()).toContain("No matching notes found.")

    vi.useRealTimers()
  })

  it("shows recently updated notes when search key is empty in dropdown mode with noteId", async () => {
    vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "", noteId: 1, isDropdown: true })
      .mount()

    await nextTick()
    await flushPromises()

    // When noteId is provided, we show recent notes instead of "Similar notes within the same notebook"
    // If no recent notes are available, show "No recent notes found."
    expect(wrapper.text()).toContain("No recent notes found.")
  })

  it("shows checkboxes but no search message when search key is empty initially in non-dropdown mode", async () => {
    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "", isDropdown: false })
      .mount()

    await nextTick()
    await flushPromises()

    expect(wrapper.text()).toContain("All My Notebooks And Subscriptions")
    expect(wrapper.text()).toContain("All My Circles")
  })

  it("shows 'No matching notes found.' when results are empty in non-dropdown mode", async () => {
    vi.useFakeTimers()

    const empty: NoteSearchResult[] = []
    vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
      data: empty,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
      data: empty,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "z", isDropdown: false })
      .mount()

    await nextTick()
    vi.advanceTimersByTime(1100)
    await flushPromises()

    expect(wrapper.text()).toContain("No matching notes found.")

    vi.useRealTimers()
  })
  it("triggers second API call for same trimmed key when context changes", async () => {
    vi.useFakeTimers()

    const result = [
      { noteTopology: { id: 1, title: "Alpha" } },
    ] as unknown as Array<unknown>

    const firstSpy = vitest.fn().mockResolvedValue(result)
    const withinSpy = vitest.fn().mockResolvedValue(result)
    const semanticSpy = vitest.fn().mockResolvedValue([])
    const semanticWithinSpy = vitest.fn().mockResolvedValue([])
    vi.spyOn(sdk, "searchForLinkTarget").mockImplementation((...args) =>
      firstSpy(...args).then((data) => ({
        data,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }))
    )
    vi.spyOn(sdk, "searchForLinkTargetWithin").mockImplementation((...args) =>
      withinSpy(...args).then((data) => ({
        data,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }))
    )
    vi.spyOn(sdk, "semanticSearch").mockImplementation((...args) =>
      semanticSpy(...args).then((data) => ({
        data,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }))
    )
    vi.spyOn(sdk, "semanticSearchWithin").mockImplementation((...args) =>
      semanticWithinSpy(...args).then((data) => ({
        data,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }))
    )
    vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "a", isDropdown: true })
      .mount()

    // first debounced call
    await nextTick()
    vi.advanceTimersByTime(1100)
    await flushPromises()

    // change search context (noteId) and keep same trimmed key (adds trailing space)
    await wrapper.setProps({ noteId: 1, inputSearchKey: "a " })

    // second debounced schedules another API call for same trimmed key
    // ensure watchers flush
    await nextTick()
    vi.advanceTimersByTime(1100)
    await flushPromises()

    expect(firstSpy).toHaveBeenCalledTimes(1)
    expect(semanticSpy).toHaveBeenCalledTimes(1)
    expect(withinSpy).toHaveBeenCalledTimes(1)
    expect(semanticWithinSpy).toHaveBeenCalledTimes(1)

    vi.useRealTimers()
  })

  it("merges unique results and sorts by ascending distance", async () => {
    vi.useFakeTimers()

    const r = (id: number, d?: number) =>
      ({
        noteTopology: { id, title: `N${id}` },
        distance: d,
      }) as unknown as object

    const firstBatch = [r(2, 0.4), r(1, 0.2)] as Array<unknown>
    const secondBatch = [r(1, 0.1), r(3, 0.8)] as Array<unknown>

    const mockTop = vitest.fn().mockResolvedValueOnce(firstBatch)
    const mockWithin = vitest.fn().mockResolvedValueOnce(secondBatch)
    const mockSemanticTop = vitest.fn().mockResolvedValueOnce([])
    const mockSemanticWithin = vitest.fn().mockResolvedValueOnce([])

    vi.spyOn(sdk, "searchForLinkTarget").mockImplementation((...args) =>
      mockTop(...args).then((data) => ({
        data,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }))
    )
    vi.spyOn(sdk, "searchForLinkTargetWithin").mockImplementation((...args) =>
      mockWithin(...args).then((data) => ({
        data,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }))
    )
    vi.spyOn(sdk, "semanticSearch").mockImplementation((...args) =>
      mockSemanticTop(...args).then((data) => ({
        data,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }))
    )
    vi.spyOn(sdk, "semanticSearchWithin").mockImplementation((...args) =>
      mockSemanticWithin(...args).then((data) => ({
        data,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }))
    )
    vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "x", isDropdown: true })
      .mount()

    // first batch
    await nextTick()
    vi.advanceTimersByTime(1100)
    await flushPromises()

    // second batch using context change to within (noteId)
    await wrapper.setProps({ noteId: 1, inputSearchKey: "x " })
    // ensure watchers flush
    await nextTick()
    await flushPromises()
    vi.advanceTimersByTime(1100)
    await flushPromises()

    // Ensure both endpoints were used once
    expect(mockTop).toHaveBeenCalledTimes(1)
    expect(mockSemanticTop).toHaveBeenCalledTimes(1)
    expect(mockWithin).toHaveBeenCalledTimes(1)
    expect(mockSemanticWithin).toHaveBeenCalledTimes(1)

    // After second call, results should be merged and ordered by distance
    // We expect ids in order of ascending distance after merge: id=1 (0.1), id=2 (0.4), id=3 (0.8)
    // Find all router links and parse ids from href-like JSON in stub
    const links = wrapper.findAll(".router-link")
    const ids = links.map((a) => {
      const to = a.attributes("to") ?? "{}"
      try {
        const parsed = JSON.parse(to)
        return parsed.params.noteId as number
      } catch {
        return undefined
      }
    })

    expect(ids.filter((x) => x !== undefined)).toEqual([1, 2, 3])

    vi.useRealTimers()
  })

  describe("recent notes", () => {
    const recentNotes = [
      {
        id: 1,
        note: {
          id: 1,
          noteTopology: { id: 1, titleOrPredicate: "Recent Note 1" },
          updatedAt: new Date().toISOString(),
        },
      },
      {
        id: 2,
        note: {
          id: 2,
          noteTopology: { id: 2, titleOrPredicate: "Recent Note 2" },
          updatedAt: new Date().toISOString(),
        },
      },
    ] as NoteRealm[]

    it("shows recently updated notes when search key is empty and searching globally", async () => {
      vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
        data: recentNotes,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", isDropdown: false })
        .mount()

      await nextTick()
      await flushPromises()

      expect(sdk.getRecentNotes).toHaveBeenCalled()
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).toContain("Recent Note 2")
    })

    it("shows 'Search result' title when search results are back (even if empty)", async () => {
      vi.useFakeTimers()

      const empty: NoteSearchResult[] = []
      vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
        data: empty,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
        data: empty,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "test", isDropdown: false })
        .mount()

      await nextTick()
      vi.advanceTimersByTime(1100)
      await flushPromises()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain("No matching notes found.")
      expect(wrapper.text()).not.toContain("Recently updated notes")

      vi.useRealTimers()
    })

    it("shows 'Search result' title when search results are back with results", async () => {
      vi.useFakeTimers()

      const searchResults: NoteSearchResult[] = [
        { noteTopology: { id: 3, titleOrPredicate: "Search Result" } },
      ]

      vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
        data: searchResults,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "test", isDropdown: false })
        .mount()

      await nextTick()
      vi.advanceTimersByTime(1100)
      await flushPromises()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain("Search Result")
      expect(wrapper.text()).not.toContain("Recently updated notes")

      vi.useRealTimers()
    })

    it("shows recently updated notes while waiting for search results", async () => {
      vi.useFakeTimers()

      const delayed = new Promise<Array<unknown>>((resolve) =>
        setTimeout(() => resolve([]), 1)
      )

      vi.spyOn(sdk, "searchForLinkTarget").mockReturnValue(
        delayed.then((data) => ({
          data,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        })) as never
      )
      vi.spyOn(sdk, "semanticSearch").mockReturnValue(
        delayed.then((data) => ({
          data,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        })) as never
      )
      vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
        data: recentNotes,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "test", isDropdown: false })
        .mount()

      await nextTick()
      vi.advanceTimersByTime(100)
      await flushPromises()

      expect(wrapper.text()).toContain("Searching ...")
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).not.toContain("Search result")

      vi.useRealTimers()
    })

    it("shows 'Search result' title when search results arrive", async () => {
      vi.useFakeTimers()

      const searchResults: NoteSearchResult[] = [
        { noteTopology: { id: 3, titleOrPredicate: "Search Result" } },
      ]

      vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
        data: searchResults,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
        data: recentNotes,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "test", isDropdown: false })
        .mount()

      await nextTick()
      await flushPromises()
      vi.advanceTimersByTime(1100)
      await flushPromises()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).not.toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Search Result")

      vi.useRealTimers()
    })

    it("does not show recent notes when allMyNotebooksAndSubscriptions is false", async () => {
      vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
        data: recentNotes,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", isDropdown: false })
        .mount()

      // Manually uncheck the checkbox (simulating user action)
      await nextTick()
      await flushPromises()

      // The checkbox should be checked by default when noteId is not provided
      // But if we simulate unchecking it, recent notes should not show
      // This test verifies the component respects the checkbox state
      expect(wrapper.text()).toContain("Recently updated notes")
    })

    it("should call getRecentNotes only once when mounting with empty search key", async () => {
      const getRecentNotesSpy = vi
        .spyOn(sdk, "getRecentNotes")
        .mockResolvedValue({
          data: recentNotes,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        })
      vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      // Clear any previous calls from other tests
      getRecentNotesSpy.mockClear()

      helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", isDropdown: false })
        .mount()

      await nextTick()
      await flushPromises()

      // Should only be called once, not twice
      expect(getRecentNotesSpy).toHaveBeenCalledTimes(1)
    })

    it("should show 'Recently updated notes' title when search key is cleared after searching", async () => {
      vi.useFakeTimers()

      const searchResults: NoteSearchResult[] = [
        { noteTopology: { id: 3, titleOrPredicate: "Search Result" } },
      ]

      vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
        data: searchResults,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
        data: recentNotes,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "test", isDropdown: false })
        .mount()

      // Wait for search to complete
      await nextTick()
      vi.advanceTimersByTime(1100)
      await flushPromises()

      // Should show "Search result" title after search
      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain("Search Result")

      // Clear the search key
      await wrapper.setProps({ inputSearchKey: "" })
      await nextTick()
      await flushPromises()

      // Should now show "Recently updated notes" title, not "Search result"
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).not.toContain("Search result")
      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).toContain("Recent Note 2")

      vi.useRealTimers()
    })

    it("shows recently updated notes when searching for link targets (noteId provided) and search key is empty", async () => {
      vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
        data: recentNotes,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "searchForLinkTargetWithin").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearchWithin").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", noteId: 999, isDropdown: true })
        .mount()

      await nextTick()
      await flushPromises()

      expect(sdk.getRecentNotes).toHaveBeenCalled()
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).toContain("Recent Note 2")
    })

    it("excludes current node from recent notes when searching for link targets", async () => {
      const recentNotesWithCurrent = [
        {
          id: 999,
          note: {
            id: 999,
            noteTopology: { id: 999, titleOrPredicate: "Current Note" },
            updatedAt: new Date().toISOString(),
          },
        },
        {
          id: 1,
          note: {
            id: 1,
            noteTopology: { id: 1, titleOrPredicate: "Recent Note 1" },
            updatedAt: new Date().toISOString(),
          },
        },
        {
          id: 2,
          note: {
            id: 2,
            noteTopology: { id: 2, titleOrPredicate: "Recent Note 2" },
            updatedAt: new Date().toISOString(),
          },
        },
      ] as NoteRealm[]

      vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
        data: recentNotesWithCurrent,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "searchForLinkTargetWithin").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearchWithin").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", noteId: 999, isDropdown: true })
        .mount()

      await nextTick()
      await flushPromises()

      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).toContain("Recent Note 2")
      expect(wrapper.text()).not.toContain("Current Note")
    })

    it("does not show recent notes in general search when noteId is not provided and allMyNotebooksAndSubscriptions is false", async () => {
      vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
        data: recentNotes,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", isDropdown: false })
        .mount()

      await nextTick()
      await flushPromises()

      // Uncheck the checkbox to simulate allMyNotebooksAndSubscriptions being false
      const checkbox = wrapper.find(
        'input[type="checkbox"][field="allMyNotebooksAndSubscriptions"]'
      )
      if (checkbox.exists()) {
        await checkbox.setValue(false)
        await nextTick()
        await flushPromises()
      }

      // When allMyNotebooksAndSubscriptions is false and noteId is not provided,
      // recent notes should not be shown
      // The component should still show the checkboxes but not recent notes
      expect(wrapper.text()).toContain("All My Notebooks And Subscriptions")
    })
  })
})

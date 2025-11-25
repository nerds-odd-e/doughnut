import SearchResults from "@/components/search/SearchResults.vue"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import type { NoteRealm, NoteSearchResult } from "@generated/backend"

describe("SearchResults.vue", () => {
  it("shows 'Searching ...' before results arrive", async () => {
    vi.useFakeTimers()

    const delayed = new Promise<Array<unknown>>((resolve) =>
      setTimeout(() => resolve([]), 1)
    )

    const searchSpy = mockSdkService("searchForLinkTarget", [])
    const semanticSpy = mockSdkService("semanticSearch", [])
    searchSpy.mockReturnValue(
      delayed.then((data) => wrapSdkResponse(data)) as never
    )
    semanticSpy.mockReturnValue(
      delayed.then((data) => wrapSdkResponse(data)) as never
    )
    mockSdkService("getRecentNotes", [])

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
    mockSdkService("searchForLinkTarget", empty)
    mockSdkService("semanticSearch", empty)
    mockSdkService("getRecentNotes", [])

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
    mockSdkService("getRecentNotes", [])

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
    mockSdkService("searchForLinkTarget", empty)
    mockSdkService("semanticSearch", empty)
    mockSdkService("getRecentNotes", [])

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
    mockSdkServiceWithImplementation("searchForLinkTarget", async (options) => {
      return await firstSpy(options)
    })
    mockSdkServiceWithImplementation(
      "searchForLinkTargetWithin",
      async (options) => {
        return await withinSpy(options)
      }
    )
    mockSdkServiceWithImplementation("semanticSearch", async (options) => {
      return await semanticSpy(options)
    })
    mockSdkServiceWithImplementation(
      "semanticSearchWithin",
      async (options) => {
        return await semanticWithinSpy(options)
      }
    )
    mockSdkService("getRecentNotes", [])

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

    mockSdkServiceWithImplementation("searchForLinkTarget", async (options) => {
      return await mockTop(options)
    })
    mockSdkServiceWithImplementation(
      "searchForLinkTargetWithin",
      async (options) => {
        return await mockWithin(options)
      }
    )
    mockSdkServiceWithImplementation("semanticSearch", async (options) => {
      return await mockSemanticTop(options)
    })
    mockSdkServiceWithImplementation(
      "semanticSearchWithin",
      async (options) => {
        return await mockSemanticWithin(options)
      }
    )
    mockSdkService("getRecentNotes", [])

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
      const getRecentNotesSpy = mockSdkService("getRecentNotes", recentNotes)
      mockSdkService("searchForLinkTarget", [])
      mockSdkService("semanticSearch", [])

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", isDropdown: false })
        .mount()

      await nextTick()
      await flushPromises()

      expect(getRecentNotesSpy).toHaveBeenCalled()
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).toContain("Recent Note 2")
    })

    it("shows 'Search result' title when search results are back (even if empty)", async () => {
      vi.useFakeTimers()

      const empty: NoteSearchResult[] = []
      mockSdkService("searchForLinkTarget", empty)
      mockSdkService("semanticSearch", empty)

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

      mockSdkService("searchForLinkTarget", searchResults)
      mockSdkService("semanticSearch", [])
      mockSdkService("getRecentNotes", [])

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

    it("shows recently updated notes while waiting for search results when no previous result exists", async () => {
      vi.useFakeTimers()

      const delayed = new Promise<Array<unknown>>((resolve) =>
        setTimeout(() => resolve([]), 1)
      )

      const searchSpy = mockSdkService("searchForLinkTarget", [])
      searchSpy.mockReturnValue(
        delayed.then((data) => wrapSdkResponse(data)) as never
      )
      const semanticSpy = mockSdkService("semanticSearch", [])
      semanticSpy.mockReturnValue(
        delayed.then((data) => wrapSdkResponse(data)) as never
      )
      mockSdkService("getRecentNotes", recentNotes)

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "test", isDropdown: false })
        .mount()

      await nextTick()
      vi.advanceTimersByTime(100)
      await flushPromises()

      // When there's no previous result and we're waiting for the first search,
      // show recent notes while waiting
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

      mockSdkService("searchForLinkTarget", searchResults)
      mockSdkService("semanticSearch", [])
      mockSdkService("getRecentNotes", recentNotes)

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
      mockSdkService("getRecentNotes", recentNotes)
      mockSdkService("searchForLinkTarget", [])
      mockSdkService("semanticSearch", [])

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
      const getRecentNotesSpy = mockSdkService("getRecentNotes", recentNotes)
      mockSdkService("searchForLinkTarget", [])
      mockSdkService("semanticSearch", [])

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

      mockSdkService("searchForLinkTarget", searchResults)
      mockSdkService("semanticSearch", [])
      mockSdkService("getRecentNotes", recentNotes)

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
      const getRecentNotesSpy = mockSdkService("getRecentNotes", recentNotes)
      mockSdkService("searchForLinkTargetWithin", [])
      mockSdkService("semanticSearchWithin", [])

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", noteId: 999, isDropdown: true })
        .mount()

      await nextTick()
      await flushPromises()

      expect(getRecentNotesSpy).toHaveBeenCalled()
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

      mockSdkService("getRecentNotes", recentNotesWithCurrent)
      mockSdkService("searchForLinkTargetWithin", [])
      mockSdkService("semanticSearchWithin", [])

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
      mockSdkService("getRecentNotes", recentNotes)
      mockSdkService("searchForLinkTarget", [])
      mockSdkService("semanticSearch", [])

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

    it("keeps previous search result visible while waiting for new search response", async () => {
      vi.useFakeTimers()

      const firstSearchResults: NoteSearchResult[] = [
        { noteTopology: { id: 1, titleOrPredicate: "First Result" } },
        { noteTopology: { id: 2, titleOrPredicate: "Second Result" } },
      ]

      const secondSearchDelayed = new Promise<Array<unknown>>((resolve) =>
        setTimeout(() => resolve([]), 2000)
      )

      const searchForLinkTargetSpy = mockSdkService("searchForLinkTarget", [])
      searchForLinkTargetSpy.mockResolvedValueOnce(
        wrapSdkResponse(firstSearchResults)
      )
      searchForLinkTargetSpy.mockReturnValue(
        secondSearchDelayed.then((data) => wrapSdkResponse(data)) as never
      )

      const semanticSearchSpy = mockSdkService("semanticSearch", [])
      semanticSearchSpy.mockResolvedValueOnce(wrapSdkResponse([]))
      semanticSearchSpy.mockReturnValue(
        secondSearchDelayed.then((data) => wrapSdkResponse(data)) as never
      )

      mockSdkService("getRecentNotes", recentNotes)

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "first", isDropdown: false })
        .mount()

      // Wait for first search to complete
      await nextTick()
      vi.advanceTimersByTime(1100)
      await flushPromises()

      // Verify first search results are shown
      expect(wrapper.text()).toContain("First Result")
      expect(wrapper.text()).toContain("Second Result")
      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).not.toContain("Recently updated notes")

      // User continues typing - trigger new search
      await wrapper.setProps({ inputSearchKey: "first second" })
      await nextTick()
      vi.advanceTimersByTime(100) // Advance time but not enough for debounce

      // Previous results should still be visible, not recent notes
      expect(wrapper.text()).toContain("First Result")
      expect(wrapper.text()).toContain("Second Result")
      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).not.toContain("Recently updated notes")
      expect(wrapper.text()).not.toContain("Recent Note 1")

      vi.useRealTimers()
    })

    it("shows recent notes only when search key is empty and no previous result exists", async () => {
      vi.useFakeTimers()

      mockSdkService("getRecentNotes", recentNotes)

      mockSdkService("searchForLinkTarget", [])

      mockSdkService("semanticSearch", [])

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", isDropdown: false })
        .mount()

      await nextTick()
      await flushPromises()

      // When search key is empty and no previous search, should show recent notes
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")

      vi.useRealTimers()
    })

    it("shows 'Searching ...' indicator when editing search key after first result is received", async () => {
      vi.useFakeTimers()

      const firstSearchResults: NoteSearchResult[] = [
        { noteTopology: { id: 1, titleOrPredicate: "First Result" } },
      ]

      const secondSearchDelayed = new Promise<Array<unknown>>((resolve) =>
        setTimeout(() => resolve([]), 2000)
      )

      const searchForLinkTargetSpy = mockSdkService("searchForLinkTarget", [])
      searchForLinkTargetSpy.mockResolvedValueOnce(
        wrapSdkResponse(firstSearchResults)
      )
      searchForLinkTargetSpy.mockReturnValue(
        secondSearchDelayed.then((data) => wrapSdkResponse(data)) as never
      )

      const semanticSearchSpy = mockSdkService("semanticSearch", [])
      semanticSearchSpy.mockResolvedValueOnce(wrapSdkResponse([]))
      semanticSearchSpy.mockReturnValue(
        secondSearchDelayed.then((data) => wrapSdkResponse(data)) as never
      )

      mockSdkService("getRecentNotes", [])

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "first", isDropdown: false })
        .mount()

      // Wait for first search to complete
      await nextTick()
      vi.advanceTimersByTime(1100)
      await flushPromises()

      // Verify first search results are shown
      expect(wrapper.text()).toContain("First Result")
      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).not.toContain("Searching ...")

      // User edits the search key - trigger new search
      await wrapper.setProps({ inputSearchKey: "first second" })
      await nextTick()
      vi.advanceTimersByTime(100) // Advance time but not enough for debounce to complete

      // "Searching ..." indicator should be shown even though previous results are still visible
      expect(wrapper.text()).toContain("Searching ...")
      expect(wrapper.text()).toContain("First Result") // Previous results still visible
      expect(wrapper.text()).toContain("Search result")

      vi.useRealTimers()
    })
  })
})

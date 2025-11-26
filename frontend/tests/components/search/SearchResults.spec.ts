import SearchResults from "@/components/search/SearchResults.vue"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import type { NoteRealm, NoteSearchResult } from "@generated/backend"

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

async function waitForSearch() {
  await nextTick()
  vi.advanceTimersByTime(1100)
  await flushPromises()
}

function setupSearchMocks(
  literalResults: NoteSearchResult[] = [],
  semanticResults: NoteSearchResult[] = []
) {
  mockSdkService("searchForLinkTarget", literalResults)
  mockSdkService("semanticSearch", semanticResults)
  mockSdkService("searchForLinkTargetWithin", literalResults)
  mockSdkService("semanticSearchWithin", semanticResults)
}

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

  it("shows 'No matching notes found.' when results are empty after search", async () => {
    vi.useFakeTimers()

    setupSearchMocks([], [])
    mockSdkService("getRecentNotes", [])

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "z", isDropdown: true })
      .mount()

    await waitForSearch()

    expect(wrapper.text()).toContain("No matching notes found.")

    vi.useRealTimers()
  })

  it("shows empty message when no recent notes available with noteId", async () => {
    mockSdkService("getRecentNotes", [])

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "", noteId: 1, isDropdown: true })
      .mount()

    await nextTick()
    await flushPromises()

    expect(wrapper.text()).toContain("No recent notes found.")
  })

  it("shows checkboxes in non-dropdown mode", async () => {
    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "", isDropdown: false })
      .mount()

    await nextTick()
    await flushPromises()

    expect(wrapper.text()).toContain("All My Notebooks And Subscriptions")
    expect(wrapper.text()).toContain("All My Circles")
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
    it("shows recently updated notes when search key is empty", async () => {
      const getRecentNotesSpy = mockSdkService("getRecentNotes", recentNotes)
      setupSearchMocks()

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

    it("shows 'Search result' title when search completes (even if empty)", async () => {
      vi.useFakeTimers()

      setupSearchMocks([], [])

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "test", isDropdown: false })
        .mount()

      await waitForSearch()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain("No matching notes found.")
      expect(wrapper.text()).not.toContain("Recently updated notes")

      vi.useRealTimers()
    })

    it("shows 'Search result' title when results are found", async () => {
      vi.useFakeTimers()

      const searchResults: NoteSearchResult[] = [
        { noteTopology: { id: 3, titleOrPredicate: "Search Result" } },
      ]

      setupSearchMocks(searchResults, [])
      mockSdkService("getRecentNotes", [])

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "test", isDropdown: false })
        .mount()

      await waitForSearch()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain("Search Result")
      expect(wrapper.text()).not.toContain("Recently updated notes")

      vi.useRealTimers()
    })

    it("shows recent notes while waiting for first search", async () => {
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

      expect(wrapper.text()).toContain("Searching ...")
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).not.toContain("Search result")

      vi.useRealTimers()
    })

    it("calls getRecentNotes only once on mount", async () => {
      const getRecentNotesSpy = mockSdkService("getRecentNotes", recentNotes)
      setupSearchMocks()

      getRecentNotesSpy.mockClear()

      helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", isDropdown: false })
        .mount()

      await nextTick()
      await flushPromises()

      expect(getRecentNotesSpy).toHaveBeenCalledTimes(1)
    })

    it("switches back to recent notes when search key is cleared", async () => {
      vi.useFakeTimers()

      const searchResults: NoteSearchResult[] = [
        { noteTopology: { id: 3, titleOrPredicate: "Search Result" } },
      ]

      setupSearchMocks(searchResults, [])
      mockSdkService("getRecentNotes", recentNotes)

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "test", isDropdown: false })
        .mount()

      await waitForSearch()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain("Search Result")

      await wrapper.setProps({ inputSearchKey: "" })
      await nextTick()
      await flushPromises()

      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).not.toContain("Search result")
      expect(wrapper.text()).toContain("Recent Note 1")

      vi.useRealTimers()
    })

    it("shows recent notes for link target search with noteId", async () => {
      const getRecentNotesSpy = mockSdkService("getRecentNotes", recentNotes)
      setupSearchMocks()

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", noteId: 999, isDropdown: true })
        .mount()

      await nextTick()
      await flushPromises()

      expect(getRecentNotesSpy).toHaveBeenCalled()
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
    })

    it("excludes current node from recent notes", async () => {
      const recentNotesWithCurrent = [
        {
          id: 999,
          note: {
            id: 999,
            noteTopology: { id: 999, titleOrPredicate: "Current Note" },
            updatedAt: new Date().toISOString(),
          },
        },
        ...recentNotes,
      ] as NoteRealm[]

      mockSdkService("getRecentNotes", recentNotesWithCurrent)
      setupSearchMocks()

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "", noteId: 999, isDropdown: true })
        .mount()

      await nextTick()
      await flushPromises()

      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).not.toContain("Current Note")
    })

    it("keeps previous results visible while waiting for new search", async () => {
      vi.useFakeTimers()

      const firstSearchResults: NoteSearchResult[] = [
        { noteTopology: { id: 1, titleOrPredicate: "First Result" } },
      ]

      const secondSearchDelayed = new Promise<Array<unknown>>((resolve) =>
        setTimeout(() => resolve([]), 2000)
      )

      const searchSpy = mockSdkService("searchForLinkTarget", [])
      searchSpy.mockResolvedValueOnce(wrapSdkResponse(firstSearchResults))
      searchSpy.mockReturnValue(
        secondSearchDelayed.then((data) => wrapSdkResponse(data)) as never
      )

      const semanticSpy = mockSdkService("semanticSearch", [])
      semanticSpy.mockResolvedValueOnce(wrapSdkResponse([]))
      semanticSpy.mockReturnValue(
        secondSearchDelayed.then((data) => wrapSdkResponse(data)) as never
      )

      mockSdkService("getRecentNotes", recentNotes)

      const wrapper = helper
        .component(SearchResults)
        .withProps({ inputSearchKey: "first", isDropdown: false })
        .mount()

      await waitForSearch()

      expect(wrapper.text()).toContain("First Result")
      expect(wrapper.text()).toContain("Search result")

      // User continues typing
      await wrapper.setProps({ inputSearchKey: "first second" })
      await nextTick()
      vi.advanceTimersByTime(100)

      // Previous results still visible, with searching indicator
      expect(wrapper.text()).toContain("Searching ...")
      expect(wrapper.text()).toContain("First Result")
      expect(wrapper.text()).not.toContain("Recent Note 1")

      vi.useRealTimers()
    })
  })
})

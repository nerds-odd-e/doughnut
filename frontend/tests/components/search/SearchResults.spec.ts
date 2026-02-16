import SearchResults from "@/components/search/SearchResults.vue"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import type { NoteSearchResult } from "@generated/backend"
import makeMe from "@tests/fixtures/makeMe"
import { describe, it, expect, vi } from "vitest"

// Test fixtures
const recentNotes: NoteSearchResult[] = [
  makeMe.aNoteSearchResult.id(1).title("Recent Note 1").distance(null).please(),
  makeMe.aNoteSearchResult.id(2).title("Recent Note 2").distance(null).please(),
]

const searchResult = (
  id: number,
  title: string,
  distance?: number
): NoteSearchResult =>
  makeMe.aNoteSearchResult.id(id).title(title).distance(distance).please()

// Test helpers
function setupSearchMocks(
  literalResults: NoteSearchResult[] = [],
  semanticResults: NoteSearchResult[] = []
) {
  mockSdkService("searchForRelationshipTarget", literalResults)
  mockSdkService("semanticSearch", semanticResults)
  mockSdkService("searchForRelationshipTargetWithin", literalResults)
  mockSdkService("semanticSearchWithin", semanticResults)
}

function setupDelayedSearchMocks() {
  const delayed = new Promise<Array<unknown>>((resolve) =>
    setTimeout(() => resolve([]), 1)
  )

  const searchSpy = mockSdkService("searchForRelationshipTarget", [])
  const semanticSpy = mockSdkService("semanticSearch", [])
  searchSpy.mockReturnValue(
    delayed.then((data) => wrapSdkResponse(data)) as never
  )
  semanticSpy.mockReturnValue(
    delayed.then((data) => wrapSdkResponse(data)) as never
  )
  return { searchSpy, semanticSpy }
}

async function waitForDebounce() {
  await nextTick()
  vi.advanceTimersByTime(1100)
  await flushPromises()
}

function mountSearchResults(props: {
  inputSearchKey: string
  isDropdown?: boolean
  noteId?: number
}) {
  return helper.component(SearchResults).withProps(props).mount()
}

describe("SearchResults.vue", () => {
  describe("search indicators", () => {
    it("shows 'Searching ...' before results arrive", async () => {
      vi.useFakeTimers()
      setupDelayedSearchMocks()
      mockSdkService("getRecentNotes", [])

      const wrapper = mountSearchResults({
        inputSearchKey: "q",
        isDropdown: true,
      })

      await nextTick()
      vi.advanceTimersByTime(100)

      expect(wrapper.text()).toContain("Searching ...")
      vi.useRealTimers()
    })

    it("shows 'No matching notes found.' when results are empty after search", async () => {
      vi.useFakeTimers()
      setupSearchMocks()
      mockSdkService("getRecentNotes", [])

      const wrapper = mountSearchResults({
        inputSearchKey: "z",
        isDropdown: true,
      })
      await waitForDebounce()

      expect(wrapper.text()).toContain("No matching notes found.")
      vi.useRealTimers()
    })
  })

  describe("checkboxes", () => {
    it("shows checkboxes in non-dropdown mode", async () => {
      const wrapper = mountSearchResults({
        inputSearchKey: "",
        isDropdown: false,
      })
      await flushPromises()

      expect(wrapper.text()).toContain("All My Notebooks And Subscriptions")
      expect(wrapper.text()).toContain("All My Circles")
    })
  })

  describe("search caching", () => {
    it("triggers second API call when context changes (noteId added)", async () => {
      vi.useFakeTimers()

      const result = [searchResult(1, "Alpha")]
      const firstSpy = vi.fn().mockResolvedValue(result)
      const withinSpy = vi.fn().mockResolvedValue(result)
      const semanticSpy = vi.fn().mockResolvedValue([])
      const semanticWithinSpy = vi.fn().mockResolvedValue([])

      mockSdkServiceWithImplementation("searchForRelationshipTarget", firstSpy)
      mockSdkServiceWithImplementation(
        "searchForRelationshipTargetWithin",
        withinSpy
      )
      mockSdkServiceWithImplementation("semanticSearch", semanticSpy)
      mockSdkServiceWithImplementation(
        "semanticSearchWithin",
        semanticWithinSpy
      )
      mockSdkService("getRecentNotes", [])

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

      const firstBatch = [
        searchResult(2, "N2", 0.4),
        searchResult(1, "N1", 0.2),
      ]
      const secondBatch = [
        searchResult(1, "N1", 0.1),
        searchResult(3, "N3", 0.8),
      ]

      const mockTop = vi.fn().mockResolvedValueOnce(firstBatch)
      const mockWithin = vi.fn().mockResolvedValueOnce(secondBatch)
      const mockSemanticTop = vi.fn().mockResolvedValueOnce([])
      const mockSemanticWithin = vi.fn().mockResolvedValueOnce([])

      mockSdkServiceWithImplementation("searchForRelationshipTarget", mockTop)
      mockSdkServiceWithImplementation(
        "searchForRelationshipTargetWithin",
        mockWithin
      )
      mockSdkServiceWithImplementation("semanticSearch", mockSemanticTop)
      mockSdkServiceWithImplementation(
        "semanticSearchWithin",
        mockSemanticWithin
      )
      mockSdkService("getRecentNotes", [])

      const wrapper = mountSearchResults({
        inputSearchKey: "x",
        isDropdown: true,
      })
      await waitForDebounce()

      await wrapper.setProps({ noteId: 1, inputSearchKey: "x " })
      await waitForDebounce()

      const links = wrapper.findAll(".router-link")
      const ids = links.map((a) => {
        const to = a.attributes("to") ?? "{}"
        try {
          return JSON.parse(to).params.noteId as number
        } catch {
          return
        }
      })

      expect(ids.filter((x) => x !== undefined)).toEqual([1, 2, 3])
      vi.useRealTimers()
    })
  })

  describe("recent notes", () => {
    it("shows recently updated notes when search key is empty", async () => {
      const getRecentNotesSpy = mockSdkService("getRecentNotes", recentNotes)
      setupSearchMocks()

      const wrapper = mountSearchResults({
        inputSearchKey: "",
        isDropdown: false,
      })
      await flushPromises()

      expect(getRecentNotesSpy).toHaveBeenCalled()
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).toContain("Recent Note 2")
    })

    it("shows empty message when no recent notes available with noteId", async () => {
      mockSdkService("getRecentNotes", [])

      const wrapper = mountSearchResults({
        inputSearchKey: "",
        noteId: 1,
        isDropdown: true,
      })
      await flushPromises()

      expect(wrapper.text()).toContain("No recent notes found.")
    })

    it("shows 'Search result' title when search completes (even if empty)", async () => {
      vi.useFakeTimers()
      setupSearchMocks()

      const wrapper = mountSearchResults({
        inputSearchKey: "test",
        isDropdown: false,
      })
      await waitForDebounce()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain("No matching notes found.")
      expect(wrapper.text()).not.toContain("Recently updated notes")

      vi.useRealTimers()
    })

    it("shows 'Search result' title when results are found", async () => {
      vi.useFakeTimers()
      setupSearchMocks([searchResult(3, "Search Result")])
      mockSdkService("getRecentNotes", [])

      const wrapper = mountSearchResults({
        inputSearchKey: "test",
        isDropdown: false,
      })
      await waitForDebounce()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain("Search Result")
      expect(wrapper.text()).not.toContain("Recently updated notes")

      vi.useRealTimers()
    })

    it("shows recent notes while waiting for first search", async () => {
      vi.useFakeTimers()
      setupDelayedSearchMocks()
      mockSdkService("getRecentNotes", recentNotes)

      const wrapper = mountSearchResults({
        inputSearchKey: "test",
        isDropdown: false,
      })

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

      mountSearchResults({ inputSearchKey: "", isDropdown: false })
      await flushPromises()

      expect(getRecentNotesSpy).toHaveBeenCalledTimes(1)
    })

    it("calls getRecentNotes only once on mount when isDropdown is true and noteId is set (like in NoteNewDialog)", async () => {
      const getRecentNotesSpy = mockSdkService("getRecentNotes", recentNotes)
      setupSearchMocks()
      getRecentNotesSpy.mockClear()

      mountSearchResults({
        inputSearchKey: "",
        isDropdown: true,
        noteId: 999,
      })
      await flushPromises()

      expect(getRecentNotesSpy).toHaveBeenCalledTimes(1)
    })

    it("switches back to recent notes when search key is cleared", async () => {
      vi.useFakeTimers()
      setupSearchMocks([searchResult(3, "Search Result")])
      mockSdkService("getRecentNotes", recentNotes)

      const wrapper = mountSearchResults({
        inputSearchKey: "test",
        isDropdown: false,
      })
      await waitForDebounce()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain("Search Result")

      await wrapper.setProps({ inputSearchKey: "" })
      await flushPromises()

      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).not.toContain("Search result")
      expect(wrapper.text()).toContain("Recent Note 1")

      vi.useRealTimers()
    })

    it("shows recent notes for relationship target search with noteId", async () => {
      const getRecentNotesSpy = mockSdkService("getRecentNotes", recentNotes)
      setupSearchMocks()

      const wrapper = mountSearchResults({
        inputSearchKey: "",
        noteId: 999,
        isDropdown: true,
      })
      await flushPromises()

      expect(getRecentNotesSpy).toHaveBeenCalled()
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
    })

    it("excludes current node from recent notes", async () => {
      const recentNotesWithCurrent: NoteSearchResult[] = [
        makeMe.aNoteSearchResult
          .id(999)
          .title("Current Note")
          .distance(null)
          .please(),
        ...recentNotes,
      ]

      mockSdkService("getRecentNotes", recentNotesWithCurrent)
      setupSearchMocks()

      const wrapper = mountSearchResults({
        inputSearchKey: "",
        noteId: 999,
        isDropdown: true,
      })
      await flushPromises()

      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).not.toContain("Current Note")
    })

    it("keeps previous results visible while waiting for new search", async () => {
      vi.useFakeTimers()

      const firstSearchResults = [searchResult(1, "First Result")]
      const secondSearchDelayed = new Promise<Array<unknown>>((resolve) =>
        setTimeout(() => resolve([]), 2000)
      )

      const searchSpy = mockSdkService("searchForRelationshipTarget", [])
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

      expect(wrapper.text()).toContain("Searching ...")
      expect(wrapper.text()).toContain("First Result")
      expect(wrapper.text()).not.toContain("Recent Note 1")

      vi.useRealTimers()
    })
  })
})

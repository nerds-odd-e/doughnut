import type { NoteSearchResult } from "@generated/donut-backend-api"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { nextTick } from "vue"
import { describe, it, expect, vi } from "vitest"
import {
  mountSearchResults,
  recentNotes,
  searchResult,
  setupDelayedSearchMocks,
  setupSearchMocks,
  waitForDebounce,
} from "./searchResultsTestSupport"

describe("SearchResults recent notes", () => {
  it("shows recently updated notes when search key is empty", async () => {
    const getRecentNotesSpy = mockSdkService(
      NoteController,
      "getRecentNotes",
      recentNotes
    )
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
    mockSdkService(NoteController, "getRecentNotes", [])

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
    mockSdkService(NoteController, "getRecentNotes", [])

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
    mockSdkService(NoteController, "getRecentNotes", recentNotes)

    const wrapper = mountSearchResults({
      inputSearchKey: "test",
      isDropdown: false,
    })

    await nextTick()
    vi.advanceTimersByTime(100)
    await flushPromises()

    expect(
      wrapper.find(".searching-indicator .daisy-loading-spinner").exists()
    ).toBe(true)
    expect(wrapper.text()).toContain("Recently updated notes")
    expect(wrapper.text()).toContain("Recent Note 1")
    expect(wrapper.text()).not.toContain("Search result")

    vi.useRealTimers()
  })

  it("calls getRecentNotes only once on mount", async () => {
    const getRecentNotesSpy = mockSdkService(
      NoteController,
      "getRecentNotes",
      recentNotes
    )
    setupSearchMocks()
    getRecentNotesSpy.mockClear()

    mountSearchResults({ inputSearchKey: "", isDropdown: false })
    await flushPromises()

    expect(getRecentNotesSpy).toHaveBeenCalledTimes(1)
  })

  it("calls getRecentNotes only once on mount when isDropdown is true and noteId is set (like in NoteNewForm)", async () => {
    const getRecentNotesSpy = mockSdkService(
      NoteController,
      "getRecentNotes",
      recentNotes
    )
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
    mockSdkService(NoteController, "getRecentNotes", recentNotes)

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
    const getRecentNotesSpy = mockSdkService(
      NoteController,
      "getRecentNotes",
      recentNotes
    )
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

    mockSdkService(NoteController, "getRecentNotes", recentNotesWithCurrent)
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
})

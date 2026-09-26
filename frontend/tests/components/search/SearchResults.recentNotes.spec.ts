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
  setupSearchResultsTests,
  waitForDebounce,
} from "./searchResultsTestSupport"

setupSearchResultsTests()

describe("SearchResults recent notes", () => {
  it.each([
    { context: "standalone search", props: { isDropdown: false } },
    {
      context: "relationship target search with noteId",
      props: { isDropdown: true, noteId: 999 },
    },
  ])(
    "loads recently updated notes once on mount for $context",
    async ({ props }) => {
      const getRecentNotesSpy = mockSdkService(
        NoteController,
        "getRecentNotes",
        recentNotes
      )

      const wrapper = mountSearchResults({ inputSearchKey: "", ...props })
      await flushPromises()

      expect(getRecentNotesSpy).toHaveBeenCalledTimes(1)
      expect(wrapper.text()).toContain("Recently updated notes")
      expect(wrapper.text()).toContain("Recent Note 1")
      expect(wrapper.text()).toContain("Recent Note 2")
    }
  )

  it("shows empty message when no recent notes available with noteId", async () => {
    const wrapper = mountSearchResults({
      inputSearchKey: "",
      noteId: 1,
      isDropdown: true,
    })
    await flushPromises()

    expect(wrapper.text()).toContain("No recent notes found.")
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

    const wrapper = mountSearchResults({
      inputSearchKey: "",
      noteId: 999,
      isDropdown: true,
    })
    await flushPromises()

    expect(wrapper.text()).toContain("Recent Note 1")
    expect(wrapper.text()).not.toContain("Current Note")
  })

  it.each([
    { found: "nothing", results: [], shows: "No matching notes found." },
    {
      found: "results",
      results: [searchResult(3, "Search Result")],
      shows: "Search Result",
    },
  ])(
    "replaces recent notes with the 'Search result' title when search finds $found",
    async ({ results, shows }) => {
      setupSearchMocks(results)
      mockSdkService(NoteController, "getRecentNotes", recentNotes)

      const wrapper = mountSearchResults({
        inputSearchKey: "test",
        isDropdown: false,
      })
      await waitForDebounce()

      expect(wrapper.text()).toContain("Search result")
      expect(wrapper.text()).toContain(shows)
      expect(wrapper.text()).not.toContain("Recently updated notes")
    }
  )

  it("shows recent notes while waiting for first search", async () => {
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
  })

  it("switches back to recent notes when search key is cleared", async () => {
    setupSearchMocks([searchResult(3, "Search Result")])
    mockSdkService(NoteController, "getRecentNotes", recentNotes)

    const wrapper = mountSearchResults({
      inputSearchKey: "test",
      isDropdown: false,
    })
    await waitForDebounce()

    expect(wrapper.text()).toContain("Search Result")

    await wrapper.setProps({ inputSearchKey: "" })
    await flushPromises()

    expect(wrapper.text()).toContain("Recently updated notes")
    expect(wrapper.text()).not.toContain("Search result")
    expect(wrapper.text()).toContain("Recent Note 1")
  })
})

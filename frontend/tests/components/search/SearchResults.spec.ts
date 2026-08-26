import {
  NoteController,
  SearchController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { nextTick } from "vue"
import { describe, it, expect, vi } from "vitest"
import {
  mountSearchResults,
  setupDelayedSearchMocks,
  setupSearchMocks,
  waitForDebounce,
} from "./searchResultsTestSupport"

describe("SearchResults.vue indicators and semantic toggle", () => {
  it("shows a loading indicator before results arrive", async () => {
    vi.useFakeTimers()
    setupDelayedSearchMocks()
    mockSdkService(NoteController, "getRecentNotes", [])

    const wrapper = mountSearchResults({
      inputSearchKey: "q",
      isDropdown: true,
    })

    await nextTick()
    vi.advanceTimersByTime(100)

    expect(
      wrapper.find(".searching-indicator .daisy-loading-spinner").exists()
    ).toBe(true)
    vi.useRealTimers()
  })

  it("shows 'No matching notes found.' when results are empty after search", async () => {
    vi.useFakeTimers()
    setupSearchMocks()
    mockSdkService(NoteController, "getRecentNotes", [])

    const wrapper = mountSearchResults({
      inputSearchKey: "z",
      isDropdown: true,
    })
    await waitForDebounce()

    expect(wrapper.text()).toContain("No matching notes found.")
    vi.useRealTimers()
  })

  it("does not call semantic search when semanticSearchEnabled is false", async () => {
    vi.useFakeTimers()
    const literalSpy = vi.fn().mockResolvedValue([])
    const semanticSpy = vi.fn().mockResolvedValue([])
    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTarget",
      literalSpy
    )
    mockSdkServiceWithImplementation(
      SearchController,
      "semanticSearch",
      semanticSpy
    )
    mockSdkService(NoteController, "getRecentNotes", [])

    mountSearchResults({
      inputSearchKey: "q",
      isDropdown: true,
      semanticSearchEnabled: false,
    })
    await waitForDebounce()

    expect(literalSpy).toHaveBeenCalledTimes(1)
    expect(semanticSpy).not.toHaveBeenCalled()
    vi.useRealTimers()
  })
})

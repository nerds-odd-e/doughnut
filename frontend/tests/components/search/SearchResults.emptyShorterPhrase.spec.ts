import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { describe, it, expect, vi } from "vitest"
import { mountSearchResults, waitForDebounce } from "./searchResultsTestSupport"

describe("SearchResults empty shorter phrase short-circuit", () => {
  it("does not search again when a shorter empty phrase is contained", async () => {
    vi.useFakeTimers()
    const literalSpy = vi.fn().mockResolvedValue([])
    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTarget",
      literalSpy
    )
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(NoteController, "getRecentNotes", [])

    const wrapper = mountSearchResults({
      inputSearchKey: "ab",
      isDropdown: true,
      semanticSearchEnabled: false,
    })
    await waitForDebounce()
    expect(literalSpy).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ inputSearchKey: "abc" })
    await waitForDebounce()

    expect(literalSpy).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain("No matching notes found.")
    vi.useRealTimers()
  })

  it("still searches when semantic search is enabled", async () => {
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

    const wrapper = mountSearchResults({
      inputSearchKey: "ab",
      isDropdown: true,
      semanticSearchEnabled: true,
    })
    await waitForDebounce()

    await wrapper.setProps({ inputSearchKey: "abc" })
    await waitForDebounce()

    expect(literalSpy).toHaveBeenCalledTimes(2)
    expect(semanticSpy).toHaveBeenCalledTimes(2)
    vi.useRealTimers()
  })

  it("still searches when the empty phrase is not contained", async () => {
    vi.useFakeTimers()
    const literalSpy = vi.fn().mockResolvedValue([])
    mockSdkServiceWithImplementation(
      SearchController,
      "searchForRelationshipTarget",
      literalSpy
    )
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(NoteController, "getRecentNotes", [])

    const wrapper = mountSearchResults({
      inputSearchKey: "ab",
      isDropdown: true,
      semanticSearchEnabled: false,
    })
    await waitForDebounce()

    await wrapper.setProps({ inputSearchKey: "xy" })
    await waitForDebounce()

    expect(literalSpy).toHaveBeenCalledTimes(2)
    vi.useRealTimers()
  })
})

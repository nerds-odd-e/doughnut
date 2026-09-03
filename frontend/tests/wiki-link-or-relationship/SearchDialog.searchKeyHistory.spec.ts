import { SearchController } from "@generated/donut-backend-api/sdk.gen"
import SearchForm from "@/components/wiki-link-or-relationship/SearchForm.vue"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import {
  appendSearchKeyToHistory,
  clearSearchKeyHistoryCookie,
  readSearchKeyHistory,
} from "@/utils/searchKeyHistoryCookie"
import { describe, expect, it } from "vitest"
import {
  historyDropdown,
  makeNoteHit,
  openSearchKeyHistoryDropdown,
  renderSearchForm,
  renderSearchFormInModal,
  renderSearchWithKeyHistory,
  setupSearchDialogFakeTimers,
  setupSearchDialogTests,
  titleEl,
  typeInSearch,
} from "./searchDialogTestSupport"

describe("SearchForm search key history", () => {
  setupSearchDialogTests()

  describe("search key recording", () => {
    setupSearchDialogFakeTimers()

    it("records trimmed search key after debounced search completes", async () => {
      clearSearchKeyHistoryCookie()
      const note = MakeMe.aNote.please()
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Hit", note.noteTopology.id + 1),
      ])
      const searchInput = await renderSearchForm({ note })
      await typeInSearch(searchInput, "  debounced-term  ")
      expect(readSearchKeyHistory()).toEqual(["debounced-term"])
    })
  })

  it("shows empty message when cookie has no entries", async () => {
    helper
      .component(SearchForm)
      .withCleanStorage()
      .withProps({ note: null })
      .render()
    await flushPromises()
    screen.getByPlaceholderText("Search")
    fireEvent.click(screen.getByTestId("search-key-history-trigger"))
    await flushPromises()
    expect(screen.getByText("No search history yet")).toBeInTheDocument()
  })

  it("lists cookie keys and fills the input when one is chosen", async () => {
    appendSearchKeyToHistory("older")
    appendSearchKeyToHistory("newer")
    const note = MakeMe.aNote.please()
    await renderSearchWithKeyHistory(note, ["older", "newer"])
    await openSearchKeyHistoryDropdown()
    fireEvent.click(screen.getByTestId("search-key-history-item-0"))
    await flushPromises()
    const input = screen.getByPlaceholderText("Search") as HTMLInputElement
    expect(input.value).toBe("newer")
  })

  it("collapses search key history when clicking the search input or a scope toggle", async () => {
    const note = MakeMe.aNote.please()
    const input = await renderSearchWithKeyHistory(note)
    const dropdown = historyDropdown()
    dropdown.open = true
    fireEvent.click(input)
    expect(dropdown.open).toBe(false)

    dropdown.open = true
    titleEl("All My Circles").click()
    expect(dropdown.open).toBe(false)
  })

  it("renders history panel inside the modal dialog and collapses on click elsewhere in that modal", async () => {
    const note = MakeMe.aNote.please()
    appendSearchKeyToHistory("older")
    await renderSearchFormInModal(note)
    await openSearchKeyHistoryDropdown()

    const dialog = document.querySelector("dialog.modal-mask")
    const panel = document.querySelector("[data-dropdown-portal-panel]")
    expect(dialog?.contains(panel)).toBe(true)

    titleEl("All My Circles").click()
    await flushPromises()
    expect(historyDropdown().open).toBe(false)
  })
})

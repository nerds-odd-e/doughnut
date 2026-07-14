import { SearchController } from "@generated/doughnut-backend-api/sdk.gen"
import SearchForm from "@/components/links/SearchForm.vue"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import {
  appendSearchKeyToHistory,
  clearSearchKeyHistoryCookie,
  readSearchKeyHistory,
} from "@/utils/searchKeyHistoryCookie"
import { afterEach, describe, expect, it, vi } from "vitest"
import {
  historyDropdown,
  makeNoteHit,
  openSearchKeyHistoryDropdown,
  renderSearchForm,
  renderSearchFormInModal,
  renderSearchWithKeyHistory,
  setupSearchDialogTests,
  titleEl,
  typeInSearch,
} from "./searchDialogTestSupport"

describe("SearchForm search key history", () => {
  setupSearchDialogTests()

  describe("search key recording", () => {
    afterEach(() => {
      vi.runOnlyPendingTimers()
      vi.useRealTimers()
    })

    beforeEach(() => {
      vi.useFakeTimers()
    })

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

  it.each([
    {
      scenario: "clicking the search input",
      click: (input: HTMLElement) => fireEvent.click(input),
    },
    {
      scenario: "clicking a search scope toggle",
      click: () => titleEl("All My Circles").click(),
    },
  ])("collapses search key history when $scenario", async ({ click }) => {
    const note = MakeMe.aNote.please()
    const input = await renderSearchWithKeyHistory(note)
    await openSearchKeyHistoryDropdown()
    click(input)
    await flushPromises()
    expect(historyDropdown().open).toBe(false)
  })

  it("collapses search key history inside a modal when clicking elsewhere in that modal", async () => {
    const note = MakeMe.aNote.please()
    appendSearchKeyToHistory("older")
    await renderSearchFormInModal(note)
    await openSearchKeyHistoryDropdown()
    titleEl("All My Circles").click()
    await flushPromises()
    expect(historyDropdown().open).toBe(false)
  })

  it("renders search key history panel inside the modal dialog so it is not covered by the dialog top layer", async () => {
    const note = MakeMe.aNote.please()
    appendSearchKeyToHistory("older")
    await renderSearchFormInModal(note)
    await openSearchKeyHistoryDropdown()
    const dialog = document.querySelector("dialog.modal-mask")
    const panel = document.querySelector("[data-dropdown-portal-panel]")
    expect(dialog?.contains(panel)).toBe(true)
  })
})

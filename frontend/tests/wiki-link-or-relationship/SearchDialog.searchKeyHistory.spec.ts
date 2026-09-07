import SearchForm from "@/components/wiki-link-or-relationship/SearchForm.vue"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "donut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import {
  seedSearchKeyHistory,
  seedEncodedSearchKeyHistory,
} from "@tests/helpers/searchKeyHistoryTestSupport"
import { describe, expect, it } from "vitest"
import {
  historyDropdown,
  historyItems,
  openSearchKeyHistoryDropdown,
  renderSearchForm,
  renderSearchFormInModal,
  renderSearchWithKeyHistory,
  setupSearchDialogTests,
  titleEl,
} from "./searchDialogTestSupport"

describe("SearchForm search key history", () => {
  setupSearchDialogTests()

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

  it.each(["%%%bad%%%", encodeURIComponent(JSON.stringify({ a: 1 })), "%7B"])(
    "ignores malformed history %s",
    async (encoded) => {
      seedEncodedSearchKeyHistory(encoded)
      await renderSearchForm({ note: null })
      await openSearchKeyHistoryDropdown()
      expect(screen.getByText("No search history yet")).toBeInTheDocument()
    }
  )

  it("lists only string entries from stored history", async () => {
    seedSearchKeyHistory(["newer", null, 42, "older"])
    await renderSearchForm({ note: null })
    await openSearchKeyHistoryDropdown()
    expect(historyItems()).toEqual(["newer", "older"])
  })

  it("lists cookie keys and fills the input when one is chosen", async () => {
    const note = MakeMe.aNote.please()
    await renderSearchWithKeyHistory(note, ["newer", "older"])
    await openSearchKeyHistoryDropdown()
    expect(historyItems()).toEqual(["newer", "older"])
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

  it("renders history panel inside the modal dialog", async () => {
    const note = MakeMe.aNote.please()
    seedSearchKeyHistory(["older"])
    await renderSearchFormInModal(note)
    await openSearchKeyHistoryDropdown()

    const dialog = document.querySelector("dialog.modal-mask")
    const panel = document.querySelector("[data-dropdown-portal-panel]")
    expect(dialog?.contains(panel)).toBe(true)
  })
})

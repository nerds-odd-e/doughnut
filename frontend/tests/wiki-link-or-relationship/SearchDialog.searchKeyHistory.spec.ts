import { SearchController } from "@generated/donut-backend-api/sdk.gen"
import SearchForm from "@/components/wiki-link-or-relationship/SearchForm.vue"
import { cleanup, fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import {
  seedSearchKeyHistory,
  seedEncodedSearchKeyHistory,
} from "@tests/helpers/searchKeyHistoryTestSupport"
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

    async function searchAndRemount(key: string) {
      const note = MakeMe.aNote.please()
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Hit", note.noteTopology.id + 1),
      ])
      const searchInput = await renderSearchForm({ note })
      await typeInSearch(searchInput, key)
      cleanup()
      await renderSearchForm({ note })
      await openSearchKeyHistoryDropdown()
    }

    it("records trimmed searches newest first and deduplicates after remount", async () => {
      seedSearchKeyHistory(["beta", "alpha", "older"])
      await searchAndRemount("  alpha  ")
      expect(historyItems()).toEqual(["alpha", "beta", "older"])
    })

    it("keeps the newest 100 entries after a completed search", async () => {
      seedSearchKeyHistory(Array.from({ length: 100 }, (_, i) => `k${99 - i}`))
      await searchAndRemount("k100")
      expect(historyItems()).toEqual(
        Array.from({ length: 100 }, (_, i) => `k${100 - i}`)
      )
    })

    it("limits a saved query to 512 characters", async () => {
      await searchAndRemount("x".repeat(600))
      expect(historyItems()).toEqual(["x".repeat(512)])
      await fireEvent.click(screen.getByTestId("search-key-history-item-0"))
      expect(
        (screen.getByPlaceholderText("Search") as HTMLInputElement).value
      ).toBe("x".repeat(512))
    })

    it.each(["", "   "])("does not record an empty search %j", async (key) => {
      await searchAndRemount(key)
      expect(screen.getByText("No search history yet")).toBeInTheDocument()
    })
  })

  function historyItems() {
    return screen
      .queryAllByTestId(/^search-key-history-item-/)
      .map((item) => item.textContent?.trim())
  }

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

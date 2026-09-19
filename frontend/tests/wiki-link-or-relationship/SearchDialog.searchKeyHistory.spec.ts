import { SearchController } from "@generated/donut-backend-api/sdk.gen"
import SearchForm from "@/components/wiki-link-or-relationship/SearchForm.vue"
import { SEARCH_KEY_HISTORY_KEY } from "@/utils/searchKeyHistory"
import { cleanup, fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { advanceSearchDebounce } from "@tests/helpers/searchDebounceTestSupport"
import {
  seedSearchKeyHistory,
  seedEncodedSearchKeyHistory,
  seedLocalSearchKeyHistory,
} from "@tests/helpers/searchKeyHistoryTestSupport"
import { afterEach, describe, expect, it, vi } from "vitest"
import {
  historyDropdown,
  historyItems,
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

  describe("search key recording", () => {
    setupSearchDialogFakeTimers()

    afterEach(() => {
      vi.restoreAllMocks()
      document.cookie = "unrelated=; Path=/; Max-Age=0"
      localStorage.removeItem("unrelated")
    })

    async function completeSearch(key: string) {
      const note = MakeMe.aNote.please()
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Hit", note.noteTopology.id + 1),
      ])
      const searchInput = await renderSearchForm({ note })
      await typeInSearch(searchInput, key)
      return note
    }

    async function searchAndRemount(key: string) {
      const note = await completeSearch(key)
      cleanup()
      await renderSearchForm({ note })
      await openSearchKeyHistoryDropdown()
    }

    it("records trimmed searches newest first and deduplicates after remount", async () => {
      seedSearchKeyHistory(["beta", "alpha", "older"])
      await searchAndRemount("  alpha  ")
      expect(historyItems()).toEqual(["alpha", "beta", "older"])
      expect(JSON.parse(localStorage.getItem(SEARCH_KEY_HISTORY_KEY)!)).toEqual(
        ["alpha", "beta", "older"]
      )
      expect(document.cookie).not.toContain(`${SEARCH_KEY_HISTORY_KEY}=`)
    })

    it.each([["current"], []])(
      "prefers current local history %j over stale legacy history",
      async (...keys) => {
        seedLocalSearchKeyHistory(keys)
        seedSearchKeyHistory(["stale"])
        await searchAndRemount("new")
        expect(historyItems()).toEqual(["new", ...keys])
      }
    )

    it("normalizes existing local history and preserves unrelated browser data", async () => {
      seedLocalSearchKeyHistory([" old ", "old", "", "z".repeat(600)])
      document.cookie = "unrelated=keep; Path=/"
      localStorage.setItem("unrelated", "keep")
      await searchAndRemount("new")
      expect(historyItems()).toEqual(["new", "old", "z".repeat(512)])
      expect(document.cookie).toContain("unrelated=keep")
      expect(localStorage.getItem("unrelated")).toBe("keep")
    })

    it.each(["not json", "{}"])(
      "falls back to legacy history for invalid local history %s",
      async (raw) => {
        localStorage.setItem(SEARCH_KEY_HISTORY_KEY, raw)
        seedSearchKeyHistory(["legacy"])
        await searchAndRemount("new")
        expect(historyItems()).toEqual(["new", "legacy"])
      }
    )

    it("still displays search results with malformed legacy history", async () => {
      seedEncodedSearchKeyHistory("%%%bad%%%")
      await completeSearch("new")
      expect(screen.getByText("Hit")).toBeInTheDocument()
    })

    it.each(["SecurityError", "QuotaExceededError"])(
      "keeps legacy history and results when storage write fails with %s",
      async (name) => {
        seedSearchKeyHistory(["legacy"])
        const cookie = document.cookie
        vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
          throw new DOMException("Storage denied", name)
        })
        await completeSearch("new")
        expect(screen.getByText("Hit")).toBeInTheDocument()
        expect(document.cookie).toBe(cookie)
        await openSearchKeyHistoryDropdown()
        expect(historyItems()).toEqual(["legacy"])
      }
    )

    it("keeps results usable when local storage access is denied without making a replacement cookie", async () => {
      vi.spyOn(window, "localStorage", "get").mockImplementation(() => {
        throw new DOMException("Storage denied", "SecurityError")
      })
      await completeSearch("new")
      expect(screen.getByText("Hit")).toBeInTheDocument()
      expect(document.cookie).not.toContain(`${SEARCH_KEY_HISTORY_KEY}=`)
      await openSearchKeyHistoryDropdown()
      expect(screen.getByText("No search history yet")).toBeInTheDocument()
    })

    it("uses legacy history when local storage reading is denied", async () => {
      seedSearchKeyHistory(["legacy"])
      vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
        throw new DOMException("Storage denied", "SecurityError")
      })
      await completeSearch("new")
      expect(screen.getByText("Hit")).toBeInTheDocument()
      vi.restoreAllMocks()
      cleanup()
      await renderSearchForm({ note: null })
      await openSearchKeyHistoryDropdown()
      expect(historyItems()).toEqual(["new", "legacy"])
    })

    it("keeps the newest 100 entries after a completed search", async () => {
      seedLocalSearchKeyHistory(
        Array.from({ length: 100 }, (_, i) => `k${99 - i}`)
      )
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
      await advanceSearchDebounce()
      expect(screen.getByText("Hit")).toBeInTheDocument()
    })

    it.each(["", "   "])("does not record an empty search %j", async (key) => {
      await searchAndRemount(key)
      expect(screen.getByText("No search history yet")).toBeInTheDocument()
    })
  })
})

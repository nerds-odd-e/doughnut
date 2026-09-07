import { SearchController } from "@generated/donut-backend-api/sdk.gen"
import { SEARCH_KEY_HISTORY_KEY } from "@/utils/searchKeyHistory"
import { cleanup, fireEvent, screen } from "@testing-library/vue"
import MakeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  seedSearchKeyHistory,
  seedEncodedSearchKeyHistory,
  seedLocalSearchKeyHistory,
} from "@tests/helpers/searchKeyHistoryTestSupport"
import { afterEach, describe, expect, it, vi } from "vitest"
import {
  historyItems,
  makeNoteHit,
  openSearchKeyHistoryDropdown,
  renderSearchForm,
  setupSearchDialogFakeTimers,
  setupSearchDialogTests,
  typeInSearch,
} from "./searchDialogTestSupport"

describe("SearchForm search history persistence", () => {
  setupSearchDialogTests()
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
    })

    it.each(["", "   "])("does not record an empty search %j", async (key) => {
      await searchAndRemount(key)
      expect(screen.getByText("No search history yet")).toBeInTheDocument()
    })
  })
})

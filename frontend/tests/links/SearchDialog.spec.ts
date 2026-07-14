import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import SearchForm from "@/components/links/SearchForm.vue"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { dispatchArrowKey } from "@tests/helpers/searchDialogKeyboardTestSupport"
import { advanceSearchDebounce } from "@tests/helpers/searchDebounceTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  allSearchResultItems,
  makeNoteHit,
  renderSearchForm,
  setupSearchDialogTests,
  titleEl,
  typeInSearch,
} from "./searchDialogTestSupport"

const searchInputId = "searchTerm-searchKey"

describe("SearchForm", () => {
  setupSearchDialogTests()

  describe("Matches / Recent list mode", () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    afterEach(() => {
      vi.runOnlyPendingTimers()
      vi.useRealTimers()
    })

    it("keeps search key and switches between Matches and Recent", async () => {
      const note = MakeMe.aNote.please()
      mockSdkService(NoteController, "getRecentNotes", [
        MakeMe.aNoteSearchResult.title("Recent Note").please(),
      ])
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Sedation", note.noteTopology.id + 100),
      ])

      const searchInput = await renderSearchForm({ note })
      expect(screen.getByText("Recently updated notes")).toBeInTheDocument()
      expect(screen.getByTestId("search-list-mode-matches")).toBeInTheDocument()
      expect(screen.getByTestId("search-list-mode-recent")).toBeInTheDocument()

      await typeInSearch(searchInput, "Sed")
      expect(screen.getByText("Search result")).toBeInTheDocument()
      expect(screen.getByText("Sedation")).toBeInTheDocument()
      expect(
        screen.queryByText("Recently updated notes")
      ).not.toBeInTheDocument()
      expect(searchInput).toHaveValue("Sed")

      fireEvent.click(screen.getByTestId("search-list-mode-recent"))
      await flushPromises()
      expect(screen.getByText("Recently updated notes")).toBeInTheDocument()
      expect(screen.getByText("Recent Note")).toBeInTheDocument()
      expect(searchInput).toHaveValue("Sed")

      fireEvent.click(screen.getByTestId("search-list-mode-matches"))
      await flushPromises()
      expect(screen.getByText("Search result")).toBeInTheDocument()
      expect(screen.getByText("Sedation")).toBeInTheDocument()
      expect(searchInput).toHaveValue("Sed")
    })

    it("allows switching to Recent when search is prefilled from a dead link", async () => {
      const note = MakeMe.aNote.please()
      mockSdkService(NoteController, "getRecentNotes", [
        MakeMe.aNoteSearchResult.title("Recent Note").please(),
      ])
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Selected Note", note.noteTopology.id + 100),
      ])

      const searchInput = await renderSearchForm({
        note,
        deadLinkPayload: {
          targetToken: "original text",
          displayText: "original text",
        },
      })
      expect(searchInput).toHaveValue("original text")
      await advanceSearchDebounce()
      expect(screen.getByText("Selected Note")).toBeInTheDocument()

      fireEvent.click(screen.getByTestId("search-list-mode-recent"))
      await flushPromises()
      expect(screen.getByText("Recently updated notes")).toBeInTheDocument()
      expect(screen.getByText("Recent Note")).toBeInTheDocument()
      expect(searchInput).toHaveValue("original text")
    })
  })

  it("Search at the top level with no note", async () => {
    helper
      .component(SearchForm)
      .withCleanStorage()
      .withProps({ note: null })
      .render()
    await flushPromises()
    screen.getByPlaceholderText("Search")
    expect(titleEl("Semantic search")).toBeInTheDocument()
    expect(titleEl("All My Notebooks And Subscriptions")).toBeDisabled()
  })

  describe("keyboard navigation", () => {
    async function renderSearchWithRecentNotes(count: number) {
      const recentNotes = Array.from({ length: count }, (_, i) =>
        MakeMe.aNoteSearchResult.title(`Recent Note ${i + 1}`).please()
      )
      mockSdkService(NoteController, "getRecentNotes", recentNotes)
      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note: null })
        .render()
      await flushPromises()
      const searchInput = screen.getByPlaceholderText("Search")
      expect(allSearchResultItems().length).toBeGreaterThanOrEqual(count)
      return searchInput
    }

    it("moves focus through results and back to search input with ArrowDown and ArrowUp", async () => {
      const searchInput = await renderSearchWithRecentNotes(2)
      const [firstItem, secondItem] = allSearchResultItems()
      expect(firstItem).toBeTruthy()
      expect(secondItem).toBeTruthy()

      searchInput.focus()
      expect(document.activeElement).toBe(searchInput)

      dispatchArrowKey("ArrowDown", searchInput)
      expect(firstItem!.contains(document.activeElement)).toBe(true)

      dispatchArrowKey("ArrowDown")
      expect(secondItem!.contains(document.activeElement)).toBe(true)

      dispatchArrowKey("ArrowUp")
      expect(firstItem!.contains(document.activeElement)).toBe(true)

      dispatchArrowKey("ArrowUp")
      expect(document.activeElement).toBe(
        document.getElementById(searchInputId)
      )
    })
  })

  it("toggle search settings", async () => {
    const note = MakeMe.aNote.please()
    helper.component(SearchForm).withCleanStorage().withProps({ note }).render()
    await flushPromises()
    titleEl("All My Circles").click()
    expect(titleEl("All My Notebooks And Subscriptions")).toHaveClass(
      "text-primary"
    )
    titleEl("All My Notebooks And Subscriptions").click()
    expect(titleEl("All My Circles")).not.toHaveClass("text-primary")
  })
})

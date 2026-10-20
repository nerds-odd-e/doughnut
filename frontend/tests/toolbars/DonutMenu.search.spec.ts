import {
  NoteController,
  SearchController,
} from "@generated/donut-backend-api/sdk.gen"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import { advanceSearchDebounce } from "@tests/helpers/searchDebounceTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  launcher,
  mountDonutMenu,
  openDonutMenu,
  panel,
  pressLauncher,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

const searchField = () => screen.getByPlaceholderText("Search")
const recentHeading = () => screen.queryByText("Recently updated notes")

const enterSearch = async () => {
  await fireEvent.focus(searchField())
  await flushPromises()
}

describe("Donut menu search", () => {
  beforeEach(() => {
    mockSdkService(NoteController, "getRecentNotes", [
      makeMe.aNoteSearchResult.title("Recent one").please(),
    ])
  })

  it("opens quietly with the search field after Account and before the footer", async () => {
    mountDonutMenu()
    await flushPromises()
    launcher().focus()
    await pressLauncher()
    await settleScheduledAutofocus()

    const field = searchField()
    expect(panel()).toContainElement(field)
    expect(
      screen.getByLabelText("Account").compareDocumentPosition(field)
    ).toBe(Node.DOCUMENT_POSITION_FOLLOWING)
    expect(field.compareDocumentPosition(screen.getByText("Donut by"))).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING
    )
    expect(document.activeElement).not.toBe(field)
    expect(recentHeading()).toBeNull()
  })

  it("shows Recent notes once the field is focused", async () => {
    await openDonutMenu()
    await enterSearch()

    expect(recentHeading()).toBeInTheDocument()
    expect(screen.getByText("Recent one")).toBeInTheDocument()
  })

  describe("with a key typed", () => {
    beforeEach(() => vi.useFakeTimers())
    afterEach(() => {
      vi.runOnlyPendingTimers()
      vi.useRealTimers()
    })

    it("shows Matches, and the panel stays open across scope toggles and list switches", async () => {
      mockSdkService(SearchController, "searchForRelationshipTarget", [
        {
          hitKind: "NOTE" as const,
          noteSearchResult: makeMe.aNoteSearchResult.title("Sedation").please(),
        },
      ])
      await openDonutMenu()
      await enterSearch()
      await fireEvent.update(searchField(), "Sed")
      await advanceSearchDebounce()

      expect(screen.getByText("Search result")).toBeInTheDocument()
      expect(screen.getByText("Sedation")).toBeInTheDocument()

      await fireEvent.click(screen.getByLabelText("All My Circles"))
      await fireEvent.click(screen.getByTestId("search-list-mode-recent"))
      await flushPromises()

      expect(panel()).not.toBeNull()
      expect(recentHeading()).toBeInTheDocument()
    })
  })

  it("offers no search to a visitor", async () => {
    await openDonutMenu(null)
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
  })
})

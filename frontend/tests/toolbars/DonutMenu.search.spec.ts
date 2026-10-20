import {
  NoteController,
  SearchController,
} from "@generated/donut-backend-api/sdk.gen"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import { advanceSearchDebounce } from "@tests/helpers/searchDebounceTestSupport"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { userEvent } from "vitest/browser"
import {
  launcher,
  mountDonutMenu,
  expectPanelClosed,
  openDonutMenu,
  panel,
  panelItemLabels,
  pressLauncher,
  recentHeading,
  router,
  searchField,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

const enterSearch = async () => {
  await fireEvent.focus(searchField())
  await flushPromises()
}

const resultLink = (title: string) => screen.getByText(title).closest("a")!

describe("Donut menu search", () => {
  const recentOne = makeMe.aNoteSearchResult.title("Recent one").please()

  beforeEach(() => {
    mockSdkService(NoteController, "getRecentNotes", [recentOne])
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

    it("opens a Matches title and closes the panel", async () => {
      const sedation = makeMe.aNoteSearchResult.title("Sedation").please()
      mockSdkService(SearchController, "searchForRelationshipTarget", [
        { hitKind: "NOTE" as const, noteSearchResult: sedation },
      ])
      await openDonutMenu()
      await enterSearch()
      await fireEvent.update(searchField(), "Sed")
      await advanceSearchDebounce()

      await fireEvent.click(resultLink("Sedation"))
      await flushPromises()

      expect(router.currentRoute.value.fullPath).toBe(
        router.resolve(noteShowLocation(sedation.noteTopology.id)).fullPath
      )
      expectPanelClosed()
    })
  })

  it("reaches the field by Tab after the panel items", async () => {
    await openDonutMenu()
    launcher().focus()
    for (const _ of panelItemLabels()) await userEvent.tab()
    await userEvent.tab()

    expect(document.activeElement).toBe(searchField())
  })

  it("moves from the field into the results with ArrowDown and back with ArrowUp, and Enter opens", async () => {
    await openDonutMenu()
    searchField().focus()
    await flushPromises()

    await userEvent.keyboard("{ArrowDown}")
    expect(document.activeElement).toBe(resultLink("Recent one"))
    await userEvent.keyboard("{ArrowUp}")
    expect(document.activeElement).toBe(searchField())

    await userEvent.keyboard("{ArrowDown}")
    await userEvent.keyboard("{Enter}")
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe(
      router.resolve(noteShowLocation(recentOne.noteTopology.id)).fullPath
    )
    expectPanelClosed()
  })

  it("closes without a route change when Enter opens the note already open", async () => {
    await router.push(noteShowLocation(recentOne.noteTopology.id))
    const currentRoute = router.currentRoute.value
    await openDonutMenu()
    await enterSearch()

    resultLink("Recent one").focus()
    await userEvent.keyboard("{Enter}")
    await flushPromises()

    expect(router.currentRoute.value).toBe(currentRoute)
    expectPanelClosed()
  })

  it("stays open for a modified result click", async () => {
    await openDonutMenu()
    await enterSearch()
    const link = resultLink("Recent one")
    link.addEventListener("click", (event) => event.preventDefault(), {
      once: true,
    })

    await fireEvent.click(link, { ctrlKey: true })
    await flushPromises()

    expect(panel()).not.toBeNull()
  })

  it("searches all notebooks, with that scope fixed on", async () => {
    await openDonutMenu()
    const allNotebooks = screen.getByLabelText("All notebooks")
    expect(allNotebooks).toHaveClass("text-primary")
    expect(allNotebooks).toBeDisabled()
  })

  it("offers no search to a visitor", async () => {
    await openDonutMenu(null)
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
  })
})

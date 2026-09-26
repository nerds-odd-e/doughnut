import { UserController } from "@generated/donut-backend-api/sdk.gen"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { fireEvent, screen } from "@testing-library/vue"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi } from "vitest"
import type { RouteLocationRaw } from "vue-router"
import { createMenuData } from "./mainMenuMocks"
import {
  createMatchMediaSpy,
  expectNavLinkPrimary,
  mountMainMenu,
  renderComponent,
  router,
  setupMainMenuTests,
  user,
} from "./mainMenuTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation")
vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./mainMenuMocks")
  return aiReplyEventSourceMockExports()
})

setupMainMenuTests()

function mockAssimilationCount(
  dueCount: number,
  assimilatedCountOfTheDay: number,
  totalUnassimilatedCount: number
) {
  return mockSdkService(
    UserController,
    "getMenuData",
    createMenuData({
      assimilationCount: {
        dueCount,
        assimilatedCountOfTheDay,
        totalUnassimilatedCount,
      },
    })
  )
}

describe("MainMenu navigation", () => {
  it("calls goToNextAssimilation from the assimilate menu action link", async () => {
    const goToNextSpy = vi.fn()
    vi.mocked(useGoToNextAssimilation).mockReturnValue({
      goToNextAssimilation: goToNextSpy,
    })

    await renderComponent()
    const assimilateLink = screen.getByLabelText("Assimilate")
    expect(assimilateLink.tagName).toBe("A")
    expect(assimilateLink.getAttribute("href")).toBeNull()

    await fireEvent.click(assimilateLink)

    expect(goToNextSpy).toHaveBeenCalled()
  })

  it.each([
    { linkLabel: "Note", route: { name: "notebooks" }, context: "notebooks" },
    {
      linkLabel: "Note",
      route: { name: "notebookPage", params: { notebookId: "1" } },
      context: "notebook page",
    },
    {
      linkLabel: "Note",
      route: {
        name: "folderPage",
        params: { notebookId: "1", folderId: "2" },
      },
      context: "folder page",
    },
    {
      linkLabel: "Note",
      route: { name: "noteShow", params: { noteId: "1" } },
      context: "note show",
    },
    {
      linkLabel: "Circles",
      route: { name: "circleShow", params: { circleId: "1" } },
      context: "circle show page",
    },
  ])(
    "applies primary nav styling to $linkLabel on $context",
    async ({ linkLabel, route }) => {
      await router.push(route as RouteLocationRaw)
      await flushPromises()
      await renderComponent()
      expectNavLinkPrimary(linkLabel)
    }
  )

  it("collapses horizontal menu when clicking outside", async () => {
    createMatchMediaSpy(false)
    mountMainMenu()

    const expandButton = screen.getByLabelText("Toggle menu")
    await fireEvent.click(expandButton)

    expect(screen.getByLabelText("Assimilate")).toBeInTheDocument()

    await fireEvent.click(document.body)

    expect(screen.getByLabelText("Toggle menu")).toBeInTheDocument()
  })
})

describe("MainMenu assimilate due count", () => {
  it("shows a combined due/total badge when there are due items", async () => {
    mockAssimilationCount(5, 0, 128)

    const { getByText } = mountMainMenu()
    await flushPromises()

    const badge = getByText("5/128")
    expect(badge).toHaveClass("due-count")
    expect(badge).toHaveAttribute(
      "title",
      "5 due today, 128 total unassimilated"
    )
    expect(
      screen.queryByTestId("assimilation-menu-progress")
    ).not.toBeInTheDocument()
  })

  it("does not show the badge when there is nothing due or backlogged", async () => {
    const { queryByText } = mountMainMenu()
    await flushPromises()

    expect(queryByText("0/0")).not.toBeInTheDocument()
  })

  it("calls getMenuData with timezone and refetches when user changes", async () => {
    const getMenuDataSpy = mockAssimilationCount(3, 0, 0)

    const { rerender } = mountMainMenu()
    await flushPromises()

    expect(getMenuDataSpy).toHaveBeenCalledWith({
      query: { timezone: timezoneParam() },
    })

    await rerender({ user: { ...user, id: 2 } })
    await flushPromises()

    expect(getMenuDataSpy).toHaveBeenCalledTimes(2)
  })
})

describe("MainMenu assimilate progress bar", () => {
  it("is hidden when daily plan is complete", async () => {
    mockAssimilationCount(0, 3, 0)

    await renderComponent()
    await flushPromises()

    expect(
      screen.queryByTestId("assimilation-menu-progress")
    ).not.toBeInTheDocument()
  })

  it("is visible midway with correct width", async () => {
    mockAssimilationCount(3, 2, 0)

    await renderComponent()
    await flushPromises()

    const progressBar = screen
      .getByLabelText("Assimilate")
      .querySelector('[data-testid="assimilation-menu-progress"]')
    expect(progressBar).toBeTruthy()

    const fill = progressBar?.querySelector(
      ".assimilation-menu-progress-fill"
    ) as HTMLElement
    expect(fill.style.width).toBe("40%")
  })
})

import { UserController } from "@generated/donut-backend-api/sdk.gen"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { screen } from "@testing-library/vue"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect } from "vitest"
import { createMenuData } from "./mainMenuMocks"
import {
  mountMainMenu,
  renderComponent,
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

describe("MainMenu assimilate", () => {
  describe("due count", () => {
    it("shows a combined due/total badge when there are due items", async () => {
      mockSdkService(
        UserController,
        "getMenuData",
        createMenuData({
          assimilationCount: {
            dueCount: 5,
            assimilatedCountOfTheDay: 0,
            totalUnassimilatedCount: 128,
          },
        })
      )

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
      const getMenuDataSpy = mockSdkService(
        UserController,
        "getMenuData",
        createMenuData({
          assimilationCount: {
            dueCount: 3,
            assimilatedCountOfTheDay: 0,
            totalUnassimilatedCount: 0,
          },
        })
      )

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

  describe("progress bar", () => {
    it("is hidden when daily plan is complete", async () => {
      mockSdkService(
        UserController,
        "getMenuData",
        createMenuData({
          assimilationCount: {
            dueCount: 0,
            assimilatedCountOfTheDay: 3,
            totalUnassimilatedCount: 0,
          },
        })
      )

      await renderComponent()
      await flushPromises()

      expect(
        screen.queryByTestId("assimilation-menu-progress")
      ).not.toBeInTheDocument()
    })

    it("is visible midway with correct width", async () => {
      mockSdkService(
        UserController,
        "getMenuData",
        createMenuData({
          assimilationCount: {
            dueCount: 3,
            assimilatedCountOfTheDay: 2,
            totalUnassimilatedCount: 0,
          },
        })
      )

      await renderComponent()
      await flushPromises()

      const assimilateLink = screen.getByLabelText("Assimilate")
      const progressBar = assimilateLink.querySelector(
        '[data-testid="assimilation-menu-progress"]'
      )
      expect(progressBar).toBeTruthy()

      const fill = progressBar?.querySelector(
        ".assimilation-menu-progress-fill"
      ) as HTMLElement
      expect(fill.style.width).toBe("40%")
    })
  })
})

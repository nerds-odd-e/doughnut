import {
  RecallsController,
  UserController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { screen } from "@testing-library/vue"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, it, expect, vi } from "vitest"
import { createMenuData, memoryTrackerLitesStub } from "./menuDataMocks"
import {
  mountDonutMenu,
  navItemOf,
  openDonutMenu,
  pressLauncher,
  setupDonutMenuTests,
  user,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

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

const recallCountOf = (label: string) =>
  navItemOf(label)?.querySelector(".recall-count")

describe("Donut menu assimilation count", () => {
  it("shows the due count titled with due today and total unassimilated", async () => {
    mockAssimilationCount(3, 0, 128)
    await openDonutMenu()

    const dueCount = navItemOf("Assimilate")?.querySelector(".due-count")
    expect(dueCount).toHaveAttribute(
      "title",
      "3 due today, 128 total unassimilated"
    )
    expect(dueCount).toHaveTextContent("3")
  })

  it("shows no due badge when nothing is due", async () => {
    mockAssimilationCount(0, 0, 128)
    await openDonutMenu()
    expect(navItemOf("Assimilate")?.querySelector(".due-count")).toBeNull()
  })

  it("fetches menu data with the timezone and refetches when the user changes", async () => {
    const getMenuData = mockAssimilationCount(3, 0, 0)

    const { rerender } = mountDonutMenu()
    await flushPromises()
    expect(getMenuData).toHaveBeenCalledWith({
      query: { timezone: timezoneParam() },
    })

    await rerender({ user: { ...user, id: 2 } })
    await flushPromises()
    expect(getMenuData).toHaveBeenCalledTimes(2)
  })

  it.each([
    { due: 5, assimilated: 0 },
    { due: 0, assimilated: 3 },
  ])(
    "hides the progress bar when due=$due and assimilated=$assimilated",
    async ({ due, assimilated }) => {
      mockAssimilationCount(due, assimilated, 0)
      await openDonutMenu()
      expect(
        screen.queryByTestId("assimilation-menu-progress")
      ).not.toBeInTheDocument()
    }
  )

  it("shows the progress bar midway with its width", async () => {
    mockAssimilationCount(3, 2, 0)
    await openDonutMenu()

    const fill = navItemOf("Assimilate")?.querySelector(
      '[data-testid="assimilation-menu-progress"] .assimilation-menu-progress-fill'
    ) as HTMLElement
    expect(fill.style.width).toBe("40%")
  })
})

describe("Donut menu recall count", () => {
  it.each([
    { linkLabel: "Recall", isRecallPaused: false },
    { linkLabel: "Resume", isRecallPaused: true },
  ])(
    "shows the to-repeat count on $linkLabel",
    async ({ linkLabel, isRecallPaused }) => {
      mockSdkService(
        UserController,
        "getMenuData",
        createMenuData({
          recallStatus: {
            toRepeat: memoryTrackerLitesStub(789),
            currentRecallWindowEndAt: "",
            totalAssimilatedCount: 0,
          },
        })
      )
      useRecallData().setIsRecallPaused(isRecallPaused)

      await openDonutMenu()

      expect(recallCountOf(linkLabel)).toHaveTextContent("789")
    }
  )

  it("shows no zero count when nothing is to repeat", async () => {
    await openDonutMenu()
    expect(screen.queryByText("0")).not.toBeInTheDocument()
  })

  it("decreases the count as recall progresses", async () => {
    const { setToRepeat, setCurrentIndex } = useRecallData()
    setToRepeat(memoryTrackerLitesStub(10))
    await openDonutMenu()
    expect(recallCountOf("Recall")).toHaveTextContent("10")

    setCurrentIndex(3)
    await flushPromises()
    expect(recallCountOf("Recall")).toHaveTextContent("7")
  })

  it.each([
    { linkLabel: "Recall", isRecallPaused: false, diligentMode: true },
    { linkLabel: "Recall", isRecallPaused: false, diligentMode: false },
    { linkLabel: "Resume", isRecallPaused: true, diligentMode: true },
  ])(
    "styles the $linkLabel count for diligentMode=$diligentMode",
    async ({ linkLabel, isRecallPaused, diligentMode }) => {
      const { setIsRecallPaused, setToRepeat, setDiligentMode } =
        useRecallData()
      setIsRecallPaused(isRecallPaused)
      setToRepeat(memoryTrackerLitesStub(5))
      setDiligentMode(diligentMode)

      await openDonutMenu()

      expect(
        recallCountOf(linkLabel)?.classList.contains("diligent-mode")
      ).toBe(diligentMode)
    }
  )

  describe("when the recall window ends", () => {
    const windowEndAt = "2026-09-29T12:00:00Z"

    afterEach(() => {
      vi.useRealTimers()
    })

    it("catches up due recalls once while the panel is closed", async () => {
      vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout", "Date"] })
      vi.setSystemTime("2026-09-29T11:00:00Z")
      mockSdkService(
        UserController,
        "getMenuData",
        createMenuData({
          recallStatus: {
            toRepeat: memoryTrackerLitesStub(1),
            currentRecallWindowEndAt: windowEndAt,
            totalAssimilatedCount: 0,
          },
        })
      )
      const recalling = mockSdkService(RecallsController, "recalling", {
        toRepeat: memoryTrackerLitesStub(2),
        currentRecallWindowEndAt: windowEndAt,
        totalAssimilatedCount: 0,
      })

      mountDonutMenu()
      await flushPromises()
      await vi.advanceTimersByTimeAsync(59 * 60 * 1000)
      expect(recalling).not.toHaveBeenCalled()

      await vi.advanceTimersByTimeAsync(2 * 60 * 1000)
      await flushPromises()
      await pressLauncher()
      expect(recallCountOf("Recall")).toHaveTextContent("2")

      await vi.advanceTimersByTimeAsync(24 * 60 * 60 * 1000)
      expect(recalling).toHaveBeenCalledTimes(1)
    })
  })
})

describe("Donut menu unread message count", () => {
  it("shows the unread message count on Messages", async () => {
    mockSdkService(
      UserController,
      "getMenuData",
      createMenuData({
        unreadMessages: [
          { id: 1, message: "first" },
          { id: 2, message: "second" },
        ],
      })
    )

    await openDonutMenu()

    expect(
      navItemOf("Messages")?.querySelector(".unread-count")
    ).toHaveTextContent("2")
  })
})

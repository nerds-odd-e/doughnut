import {
  RecallsController,
  UserController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import { screen } from "@testing-library/vue"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi, afterEach } from "vitest"
import {
  createMenuData,
  createUseRecallDataMock,
  memoryTrackerLitesStub,
} from "./mainMenuMocks"
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

describe("MainMenu recall count", () => {
  it.each([
    { linkLabel: "Recall", isRecallPaused: false },
    { linkLabel: "Resume", isRecallPaused: true },
  ])(
    "shows recall count on $linkLabel when there are items to repeat",
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

      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused,
          toRepeat: memoryTrackerLitesStub(789),
        })
      )

      mountMainMenu()
      await flushPromises()

      const link = screen.getByLabelText(linkLabel)
      const recallCount = link
        .closest(".nav-item")
        ?.querySelector(".recall-count")
      expect(recallCount).toHaveTextContent("789")
    }
  )

  it("does not show recall count when there are no items to repeat", async () => {
    const { queryByText } = mountMainMenu()
    await flushPromises()

    expect(queryByText("0")).not.toBeInTheDocument()
  })

  it("decreases recall count when currentIndex increases", async () => {
    const mockData = createUseRecallDataMock({
      toRepeat: memoryTrackerLitesStub(10),
      currentIndex: 0,
    })

    vi.mocked(useRecallData).mockReturnValue(mockData)

    const { getAllByText, rerender } = mountMainMenu()
    await flushPromises()

    expect(getAllByText("10").length).toBeGreaterThan(0)

    mockData.currentIndex.value = 3
    await rerender({ user })
    await flushPromises()

    expect(getAllByText("7").length).toBeGreaterThan(0)
    expect(screen.queryByText("10")).not.toBeInTheDocument()
  })

  it.each([
    {
      linkLabel: "Recall",
      isRecallPaused: false,
      diligentMode: true,
      expectDiligent: true,
    },
    {
      linkLabel: "Recall",
      isRecallPaused: false,
      diligentMode: false,
      expectDiligent: false,
    },
    {
      linkLabel: "Resume",
      isRecallPaused: true,
      diligentMode: true,
      expectDiligent: true,
    },
  ])(
    "applies diligent-mode class on $linkLabel badge when diligentMode=$diligentMode",
    async ({ linkLabel, isRecallPaused, diligentMode, expectDiligent }) => {
      vi.mocked(useRecallData).mockReturnValue(
        createUseRecallDataMock({
          isRecallPaused,
          toRepeat: memoryTrackerLitesStub(5),
          diligentMode,
        })
      )

      await renderComponent()

      const link = screen.getByLabelText(linkLabel)
      const count = link.closest(".nav-item")?.querySelector(".recall-count")
      expect(count?.classList.contains("diligent-mode")).toBe(expectDiligent)
    }
  )

  describe("when the recall window ends", () => {
    const windowEndAt = "2026-09-29T12:00:00Z"

    afterEach(() => {
      vi.useRealTimers()
    })

    it("catches up due recalls once and updates the count", async () => {
      vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout", "Date"] })
      vi.setSystemTime("2026-09-29T11:00:00Z")
      const { useRecallData: realUseRecallData } = await vi.importActual<
        typeof import("@/composables/useRecallData")
      >("@/composables/useRecallData")
      vi.mocked(useRecallData).mockImplementation(realUseRecallData)
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

      await renderComponent()
      await flushPromises()
      await vi.advanceTimersByTimeAsync(59 * 60 * 1000)
      expect(recalling).not.toHaveBeenCalled()

      await vi.advanceTimersByTimeAsync(2 * 60 * 1000)
      await flushPromises()
      const count = screen
        .getByLabelText("Recall")
        .closest(".nav-item")
        ?.querySelector(".recall-count")
      expect(count).toHaveTextContent("2")

      await vi.advanceTimersByTimeAsync(24 * 60 * 60 * 1000)
      expect(recalling).toHaveBeenCalledTimes(1)
    })
  })
})

import { UserController } from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import { screen } from "@testing-library/vue"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi } from "vitest"
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
})

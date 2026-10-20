import {
  RecallsController,
  UserController,
} from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, it, expect, vi } from "vitest"
import { createMenuData, memoryTrackerLitesStub } from "./menuDataMocks"
import {
  mountDonutMenu,
  pressLauncher,
  recallCountOf,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

describe("Donut menu recall count when the recall window ends", () => {
  const windowEndAt = "2026-09-29T12:00:00Z"

  afterEach(() => {
    vi.useRealTimers()
  })

  const mountWithWindowEnd = async () => {
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
    return recalling
  }

  const becomeVisibleAt = async (time: string) => {
    vi.setSystemTime(time)
    vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible")
    document.dispatchEvent(new Event("visibilitychange"))
    await flushPromises()
  }

  it("catches up due recalls once while the panel is closed", async () => {
    const recalling = await mountWithWindowEnd()

    await vi.advanceTimersByTimeAsync(59 * 60 * 1000)
    expect(recalling).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(2 * 60 * 1000)
    await flushPromises()
    await pressLauncher()
    expect(recallCountOf("Recall")).toHaveTextContent("2")

    await vi.advanceTimersByTimeAsync(24 * 60 * 60 * 1000)
    expect(recalling).toHaveBeenCalledTimes(1)
  })

  it("catches up once when the tab becomes visible after the window end", async () => {
    const recalling = await mountWithWindowEnd()

    await becomeVisibleAt("2026-09-29T13:00:00Z")
    await pressLauncher()

    expect(recalling).toHaveBeenCalledTimes(1)
    expect(recallCountOf("Recall")).toHaveTextContent("2")
  })

  it("does not catch up when the tab becomes visible before the window end", async () => {
    const recalling = await mountWithWindowEnd()

    await becomeVisibleAt("2026-09-29T11:30:00Z")

    expect(recalling).not.toHaveBeenCalled()
  })
})

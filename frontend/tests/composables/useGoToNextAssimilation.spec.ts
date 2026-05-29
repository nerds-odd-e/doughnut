import { describe, it, expect, beforeEach, vi } from "vitest"
import { AssimilationController } from "@generated/doughnut-backend-api/sdk.gen"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import {
  resetAssimilationViewForTests,
  useAssimilationView,
} from "@/composables/useAssimilationView"
import { mockSdkService } from "@tests/helpers"

const routerPush = vi.fn()

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock("@/managedApi/window/timezoneParam", () => ({
  default: () => "Asia/Shanghai",
}))

describe("useGoToNextAssimilation", () => {
  beforeEach(() => {
    routerPush.mockReset()
    resetAssimilationViewForTests()
    const {
      setDueCount,
      setAssimilatedCountOfTheDay,
      setTotalUnassimilatedCount,
    } = useAssimilationCount()
    setDueCount(undefined)
    setAssimilatedCountOfTheDay(undefined)
    setTotalUnassimilatedCount(undefined)
  })

  it("updates counts, enables settings, and navigates when nextNoteId is returned", async () => {
    mockSdkService(AssimilationController, "next", {
      nextNoteId: 42,
      counts: {
        dueCount: 2,
        assimilatedCountOfTheDay: 1,
        totalUnassimilatedCount: 5,
      },
    })

    const { goToNextAssimilation } = useGoToNextAssimilation()
    await goToNextAssimilation()

    const { dueCount, assimilatedCountOfTheDay, totalUnassimilatedCount } =
      useAssimilationCount()
    expect(dueCount.value).toBe(2)
    expect(assimilatedCountOfTheDay.value).toBe(1)
    expect(totalUnassimilatedCount.value).toBe(5)

    const { showAssimilationSettings, pendingOnForNoteId } =
      useAssimilationView()
    expect(showAssimilationSettings.value).toBe(true)
    expect(pendingOnForNoteId.value).toBe(42)

    expect(routerPush).toHaveBeenCalledWith({
      name: "noteShow",
      params: { noteId: "42" },
    })
  })

  it("updates counts but does not navigate when nextNoteId is null", async () => {
    mockSdkService(AssimilationController, "next", {
      nextNoteId: undefined,
      counts: {
        dueCount: 0,
        assimilatedCountOfTheDay: 3,
        totalUnassimilatedCount: 0,
      },
    })

    const { goToNextAssimilation } = useGoToNextAssimilation()
    await goToNextAssimilation()

    const { dueCount, assimilatedCountOfTheDay } = useAssimilationCount()
    expect(dueCount.value).toBe(0)
    expect(assimilatedCountOfTheDay.value).toBe(3)
    expect(routerPush).not.toHaveBeenCalled()

    const { showAssimilationSettings } = useAssimilationView()
    expect(showAssimilationSettings.value).toBe(false)
  })
})

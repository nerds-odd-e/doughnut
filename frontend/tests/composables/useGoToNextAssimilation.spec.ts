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
const showSuccessToast = vi.fn()

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

vi.mock("@/composables/useToast", () => ({
  useToast: () => ({
    showSuccessToast,
    showErrorToast: vi.fn(),
  }),
}))

describe("useGoToNextAssimilation", () => {
  beforeEach(() => {
    routerPush.mockReset()
    showSuccessToast.mockReset()
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
    expect(showSuccessToast).not.toHaveBeenCalled()
  })

  it("shows daily goal toast and navigates when dueCount is zero but next note exists", async () => {
    mockSdkService(AssimilationController, "next", {
      nextNoteId: 42,
      counts: {
        dueCount: 0,
        assimilatedCountOfTheDay: 2,
        totalUnassimilatedCount: 3,
      },
    })

    const { goToNextAssimilation } = useGoToNextAssimilation()
    await goToNextAssimilation()

    expect(showSuccessToast).toHaveBeenCalledWith(
      "You've achieved your daily assimilation goal"
    )
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
    expect(showSuccessToast).toHaveBeenCalledWith("No more notes to assimilate")

    const { showAssimilationSettings } = useAssimilationView()
    expect(showAssimilationSettings.value).toBe(false)
  })
})

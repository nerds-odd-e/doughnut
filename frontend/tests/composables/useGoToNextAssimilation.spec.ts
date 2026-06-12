import { describe, it, expect, beforeEach, vi } from "vitest"
import { AssimilationController } from "@generated/doughnut-backend-api/sdk.gen"
import {
  DAILY_GOAL_TOAST,
  NO_MORE_TOAST,
  useGoToNextAssimilation,
} from "@/composables/useGoToNextAssimilation"
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
    const navigated = await goToNextAssimilation()
    expect(navigated).toBe(true)

    const { dueCount, assimilatedCountOfTheDay, totalUnassimilatedCount } =
      useAssimilationCount()
    expect(dueCount.value).toBe(2)
    expect(assimilatedCountOfTheDay.value).toBe(1)
    expect(totalUnassimilatedCount.value).toBe(5)

    const { showAssimilationSettings, pendingOnForNoteId, pendingPropertyKey } =
      useAssimilationView()
    expect(showAssimilationSettings.value).toBe(true)
    expect(pendingOnForNoteId.value).toBe(42)
    expect(pendingPropertyKey.value).toBeNull()

    expect(routerPush).toHaveBeenCalledWith({
      name: "noteShow",
      params: { noteId: "42" },
    })
    expect(showSuccessToast).not.toHaveBeenCalled()
  })

  it("stores pending property key when nextPropertyKey is returned", async () => {
    mockSdkService(AssimilationController, "next", {
      nextNoteId: 42,
      nextPropertyKey: "example of",
      counts: {
        dueCount: 1,
        assimilatedCountOfTheDay: 1,
        totalUnassimilatedCount: 1,
      },
    })

    const { goToNextAssimilation } = useGoToNextAssimilation()
    await goToNextAssimilation()

    const { pendingOnForNoteId, pendingPropertyKey } = useAssimilationView()
    expect(pendingOnForNoteId.value).toBe(42)
    expect(pendingPropertyKey.value).toBe("example of")
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
    const navigated = await goToNextAssimilation()
    expect(navigated).toBe(true)

    expect(showSuccessToast).toHaveBeenCalledWith(DAILY_GOAL_TOAST)
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
    const navigated = await goToNextAssimilation()
    expect(navigated).toBe(false)

    const { dueCount, assimilatedCountOfTheDay } = useAssimilationCount()
    expect(dueCount.value).toBe(0)
    expect(assimilatedCountOfTheDay.value).toBe(3)
    expect(routerPush).not.toHaveBeenCalled()
    expect(showSuccessToast).toHaveBeenCalledWith(NO_MORE_TOAST)

    const { showAssimilationSettings } = useAssimilationView()
    expect(showAssimilationSettings.value).toBe(false)
  })
})

import { useRecallData } from "@/composables/useRecallData"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi } from "vitest"
import { memoryTrackerLitesStub } from "./menuDataMocks"
import {
  navItemOf,
  openDonutMenu,
  panelItemLabels,
  router,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

describe("Donut menu Resume availability", () => {
  it("lists Resume first and highlighted while recall is paused, and drops it once recall is no longer paused", async () => {
    const { setIsRecallPaused, setToRepeat } = useRecallData()
    setIsRecallPaused(true)
    setToRepeat(memoryTrackerLitesStub(5))
    await openDonutMenu()
    expect(panelItemLabels()[0]).toBe("Resume")
    expect(navItemOf("Resume")).toHaveClass("resume-recall-active")

    setIsRecallPaused(false)
    await flushPromises()
    expect(panelItemLabels()[0]).toBe("Note")
  })

  it.each([
    {
      description: "currentIndex > 0 and not on recall page",
      routeName: "notebooks",
      isRecallPaused: false,
      currentIndex: 1,
      toRepeat: memoryTrackerLitesStub(5),
      shouldShow: true,
    },
    {
      description: "currentIndex > 0 but on recall page",
      routeName: "recall",
      isRecallPaused: false,
      currentIndex: 1,
      toRepeat: undefined,
      shouldShow: false,
    },
    {
      description: "currentIndex is 0 and not on recall page",
      routeName: "notebooks",
      isRecallPaused: false,
      currentIndex: 0,
      toRepeat: undefined,
      shouldShow: false,
    },
    {
      description: "both isRecallPaused and currentIndex > 0",
      routeName: "notebooks",
      isRecallPaused: true,
      currentIndex: 2,
      toRepeat: memoryTrackerLitesStub(5),
      shouldShow: true,
    },
    {
      description: "paused with nothing to repeat",
      routeName: "notebooks",
      isRecallPaused: true,
      currentIndex: 0,
      toRepeat: [] as ReturnType<typeof memoryTrackerLitesStub>,
      shouldShow: false,
    },
    {
      description: "currentIndex past everything to repeat",
      routeName: "notebooks",
      isRecallPaused: false,
      currentIndex: 5,
      toRepeat: memoryTrackerLitesStub(5),
      shouldShow: false,
    },
  ])(
    "Resume visibility: $description → $shouldShow",
    async ({
      routeName,
      isRecallPaused,
      currentIndex,
      toRepeat,
      shouldShow,
    }) => {
      await router.push({ name: routeName })
      await flushPromises()
      const { setIsRecallPaused, setCurrentIndex, setToRepeat } =
        useRecallData()
      setIsRecallPaused(isRecallPaused)
      setCurrentIndex(currentIndex)
      setToRepeat(toRepeat)

      await openDonutMenu()

      expect(panelItemLabels().includes("Resume")).toBe(shouldShow)
    }
  )
})

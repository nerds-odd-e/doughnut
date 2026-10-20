import { useRecallCatchUp } from "@/composables/useRecallCatchUp"
import { useRecallData } from "@/composables/useRecallData"
import {
  DailyProbeController,
  MemoryTrackerController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import { ref } from "vue"
import {
  createMemoryTrackerLite,
  givenRecallQueue,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

const ctx = useRecallPageSpecContext()

// Window ends in Asia/Shanghai (UTC+8), the spec context's browser zone.
const day1NoonEnd = "2026-03-10T04:00:00Z"
const day1MidnightEnd = "2026-03-10T16:00:00Z"
const day2NoonEnd = "2026-03-11T04:00:00Z"

describe("RecallPage Daily probe when the catch-up enters a new day", () => {
  let getToday: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    getToday = mockSdkService(DailyProbeController, "getDailyProbeToday", {
      completed: true,
    })
  })

  const mountRecallWithWindowEnd = async (windowEndAt: string) => {
    ctx.renderer.withCurrentUserRef(
      ref(makeMe.aUser.dailyProbeEnabled(true).please())
    )
    givenRecallQueue(
      createMemoryTrackerLite(1),
      createMemoryTrackerLite(2),
      createMemoryTrackerLite(3)
    )
    useRecallData().setCurrentRecallWindowEndAt(windowEndAt)
    return ctx.mountPage()
  }

  const catchUpTo = async (windowEndAt: string, probeCompleted: boolean) => {
    getToday.mockResolvedValue(wrapSdkResponse({ completed: probeCompleted }))
    ctx.recallingSpy.mockResolvedValue(
      wrapSdkResponse({
        toRepeat: [],
        currentRecallWindowEndAt: windowEndAt,
        totalAssimilatedCount: 0,
      })
    )
    await useRecallCatchUp().catchUpDueRecalls()
    await flushPromises()
  }

  const showsEntry = (wrapper: VueWrapper) =>
    wrapper.find('[data-testid="daily-probe-entry"]').exists()
  const showsDailyProbe = (wrapper: VueWrapper) =>
    wrapper.findComponent({ name: "DailyProbe" }).exists()
  const choose = async (wrapper: VueWrapper, choice: "start" | "skip") => {
    await wrapper.find(`[data-testid="daily-probe-${choice}"]`).trigger("click")
    await flushPromises()
  }
  const skip = (wrapper: VueWrapper) => choose(wrapper, "skip")

  it("asks again when the catch-up enters a new local day", async () => {
    const wrapper = await mountRecallWithWindowEnd(day1MidnightEnd)

    await catchUpTo(day2NoonEnd, false)

    expect(getToday).toHaveBeenCalledTimes(2)
    expect(showsEntry(wrapper)).toBe(true)
  })

  it("does not ask again at noon, even after Skip", async () => {
    getToday.mockResolvedValue(wrapSdkResponse({ completed: false }))
    const wrapper = await mountRecallWithWindowEnd(day1NoonEnd)
    await skip(wrapper)

    await catchUpTo(day1MidnightEnd, false)

    expect(getToday).toHaveBeenCalledTimes(1)
    expect(showsEntry(wrapper)).toBe(false)
  })

  it("asks again on a new day after Skip on the day before", async () => {
    getToday.mockResolvedValue(wrapSdkResponse({ completed: false }))
    const wrapper = await mountRecallWithWindowEnd(day1MidnightEnd)
    await skip(wrapper)

    await catchUpTo(day2NoonEnd, false)

    expect(showsEntry(wrapper)).toBe(true)
  })

  it("returns to the same question after Skip when the new day interrupts mid-question", async () => {
    const getRecallPrompt = mockSdkService(
      MemoryTrackerController,
      "getRecallPrompt",
      makeMe.aRecallPrompt.please()
    )
    const wrapper = await mountRecallWithWindowEnd(day1MidnightEnd)
    ;(wrapper.vm as unknown as { currentIndex: number }).currentIndex = 1
    await flushPromises()

    await catchUpTo(day2NoonEnd, false)
    expect(showsEntry(wrapper)).toBe(true)
    getRecallPrompt.mockClear()
    await skip(wrapper)

    expect(useRecallData().currentIndex.value).toBe(1)
    expect(getRecallPrompt).toHaveBeenCalledWith(
      expect.objectContaining({ path: { memoryTracker: 2 } })
    )
  })

  it("keeps Daily probe trials running when the new day arrives during them", async () => {
    getToday.mockResolvedValue(wrapSdkResponse({ completed: false }))
    const wrapper = await mountRecallWithWindowEnd(day1MidnightEnd)
    await choose(wrapper, "start")

    await catchUpTo(day2NoonEnd, false)

    expect(showsDailyProbe(wrapper)).toBe(true)
    expect(showsEntry(wrapper)).toBe(false)
  })

  it("asks to start or skip on the new day after completing Daily probe the day before", async () => {
    getToday.mockResolvedValue(wrapSdkResponse({ completed: false }))
    const wrapper = await mountRecallWithWindowEnd(day1MidnightEnd)
    await choose(wrapper, "start")
    wrapper.findComponent({ name: "DailyProbe" }).vm.$emit("complete")
    await flushPromises()

    await catchUpTo(day2NoonEnd, false)

    expect(showsEntry(wrapper)).toBe(true)
    expect(showsDailyProbe(wrapper)).toBe(false)
  })
})

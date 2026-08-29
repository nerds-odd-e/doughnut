import { useRecallData } from "@/composables/useRecallData"
import { DailyProbeController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import { ref } from "vue"
import {
  createMemoryTrackerLite,
  createUseRecallDataMock,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/components/commons/Popups/usePopups")

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({ path: "/", fullPath: "/" }),
    useRouter: () => ({ currentRoute: { value: { name: "recall" } } }),
  }
})

describe("RecallPage Daily probe entry", () => {
  const ctx = useRecallPageSpecContext({ fakeTimers: true })

  const mountRecall = async (dailyProbeEnabled: boolean) => {
    ctx.renderer.withCurrentUserRef(
      ref(makeMe.aUser.dailyProbeEnabled(dailyProbeEnabled).please())
    )
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(1)],
      })
    )
    return ctx.mountPage()
  }

  it("shows Daily probe instead of the quiz when the learner has opted in", async () => {
    const wrapper = await mountRecall(true)

    expect(wrapper.findComponent({ name: "DailyProbe" }).exists()).toBe(true)
    expect(wrapper.findComponent({ name: "Quiz" }).exists()).toBe(false)
  })

  it("loads ordinary recall when Daily probe is off", async () => {
    const wrapper = await mountRecall(false)

    expect(wrapper.findComponent({ name: "DailyProbe" }).exists()).toBe(false)
    expect(wrapper.findComponent({ name: "Quiz" }).exists()).toBe(true)
  })

  it("skips Daily probe when today's run is already completed", async () => {
    mockSdkService(DailyProbeController, "getDailyProbeToday", {
      completed: true,
    })
    const wrapper = await mountRecall(true)

    expect(wrapper.findComponent({ name: "DailyProbe" }).exists()).toBe(false)
    expect(wrapper.findComponent({ name: "Quiz" }).exists()).toBe(true)
  })

  it("shows retry when today's Daily probe check fails", async () => {
    mockSdkService(DailyProbeController, "getDailyProbeToday", {
      completed: false,
    }).mockResolvedValue(wrapSdkError("unavailable"))
    const wrapper = await mountRecall(true)

    expect(
      wrapper.find('[data-testid="daily-probe-offer-retry"]').exists()
    ).toBe(true)
    expect(wrapper.findComponent({ name: "DailyProbe" }).exists()).toBe(false)
    expect(wrapper.findComponent({ name: "Quiz" }).exists()).toBe(false)
  })

  it("offers Daily probe after retry succeeds with today's run still due", async () => {
    const getToday = mockSdkService(
      DailyProbeController,
      "getDailyProbeToday",
      {
        completed: false,
      }
    )
    getToday.mockResolvedValueOnce(wrapSdkError("unavailable"))
    const wrapper = await mountRecall(true)
    await wrapper
      .find('[data-testid="daily-probe-offer-retry"]')
      .trigger("click")
    await flushPromises()

    expect(getToday).toHaveBeenCalledTimes(2)
    expect(wrapper.findComponent({ name: "DailyProbe" }).exists()).toBe(true)
  })
})

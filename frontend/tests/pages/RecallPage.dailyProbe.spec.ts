import { useRecallData } from "@/composables/useRecallData"
import { DailyProbeController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
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

  it("shows Daily probe instead of the quiz when the learner has opted in", async () => {
    ctx.renderer.withCurrentUserRef(
      ref(makeMe.aUser.dailyProbeEnabled(true).please())
    )
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(1)],
      })
    )

    const wrapper = await ctx.mountPage()

    expect(wrapper.findComponent({ name: "DailyProbe" }).exists()).toBe(true)
    expect(wrapper.findComponent({ name: "Quiz" }).exists()).toBe(false)
  })

  it("loads ordinary recall when Daily probe is off", async () => {
    ctx.renderer.withCurrentUserRef(
      ref(makeMe.aUser.dailyProbeEnabled(false).please())
    )
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(1)],
      })
    )

    const wrapper = await ctx.mountPage()

    expect(wrapper.findComponent({ name: "DailyProbe" }).exists()).toBe(false)
    expect(wrapper.findComponent({ name: "Quiz" }).exists()).toBe(true)
  })

  it("skips Daily probe when today's run is already completed", async () => {
    mockSdkService(DailyProbeController, "getDailyProbeToday", {
      completed: true,
    })
    ctx.renderer.withCurrentUserRef(
      ref(makeMe.aUser.dailyProbeEnabled(true).please())
    )
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(1)],
      })
    )

    const wrapper = await ctx.mountPage()

    expect(wrapper.findComponent({ name: "DailyProbe" }).exists()).toBe(false)
    expect(wrapper.findComponent({ name: "Quiz" }).exists()).toBe(true)
  })
})

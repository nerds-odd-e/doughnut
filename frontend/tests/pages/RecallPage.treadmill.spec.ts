import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { MemoryTrackerLite } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { describe, expect, it, vi, beforeEach } from "vitest"
import {
  createMemoryTrackerLite,
  createUseRecallDataMock,
  toggleTreadmillMode,
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

describe("RecallPage treadmill mode", () => {
  const normalId = 123
  const spellingId = 456
  const anotherNormalId = 789
  const ctx = useRecallPageSpecContext({ fakeTimers: true })
  let askAQuestionSpy: ReturnType<typeof mockSdkService>

  const defaultTrackers = () => [
    createMemoryTrackerLite(normalId, false),
    createMemoryTrackerLite(spellingId, true),
    createMemoryTrackerLite(anotherNormalId, false),
  ]

  beforeEach(() => {
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.please()
    )
    askAQuestionSpy = mockSdkService(
      MemoryTrackerController,
      "askAQuestion",
      makeMe.aRecallQuestion.please()
    )
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({ toRepeat: defaultTrackers() })
    )
  })

  it("should show treadmill mode toggle in settings", async () => {
    const wrapper = await ctx.mountPage()
    await wrapper.find(".progress-bar").trigger("click")
    await wrapper.vm.$nextTick()
    expect(document.body.querySelector('input[type="checkbox"]')).toBeTruthy()
    expect(document.body.textContent).toContain("Treadmill mode")
  })

  it("should skip spelling memory trackers when treadmill mode is enabled", async () => {
    const wrapper = await ctx.mountPage()
    const globalBar = wrapper.findComponent({ name: "GlobalBar" })
    expect(globalBar.text()).toContain("0/3")
    await toggleTreadmillMode(wrapper, true)
    expect(globalBar.text()).toContain("0/2")
    expect(askAQuestionSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { memoryTracker: normalId },
      })
    )
  })

  it("should apply sportive background to GlobalBar when treadmill mode is enabled", async () => {
    const wrapper = await ctx.mountPage()
    await toggleTreadmillMode(wrapper, true)
    expect(wrapper.findComponent({ name: "GlobalBar" }).classes()).toContain(
      "treadmill-mode"
    )
  })

  it("should not show spelling questions when treadmill mode is enabled", async () => {
    const wrapper = await ctx.mountPage()
    await toggleTreadmillMode(wrapper, true)
    type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    expect(vm.toRepeat?.[vm.currentIndex]?.spelling).toBe(false)
  })

  it("should update progress bar to exclude spelling memory trackers", async () => {
    const wrapper = await ctx.mountPage()
    const globalBar = wrapper.findComponent({ name: "GlobalBar" })
    expect(globalBar.text()).toMatch(/0\/[23]/)
    await toggleTreadmillMode(wrapper, true)
    expect(globalBar.text()).toContain("0/2")
  })

  it("should not add answered questions back to the list when toggling treadmill mode", async () => {
    const wrapper = await ctx.mountPage()
    type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    vm.currentIndex = 1
    await toggleTreadmillMode(wrapper, true)
    expect(vm.currentIndex).toBeGreaterThan(0)
  })

  it("should not reset currentIndex to 0 when toggling treadmill mode", async () => {
    const wrapper = await ctx.mountPage()
    type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    vm.currentIndex = 2
    await toggleTreadmillMode(wrapper, true)
    expect(vm.currentIndex).toBeGreaterThan(0)
    const currentTracker = vm.toRepeat?.[vm.currentIndex]
    if (currentTracker) expect(currentTracker.spelling).toBe(false)
  })

  it("should move unanswered spelling memory trackers to the end when treadmill mode is turned off", async () => {
    const fourthNormalId = 111
    const trackers = [
      createMemoryTrackerLite(normalId, false),
      createMemoryTrackerLite(anotherNormalId, false),
      createMemoryTrackerLite(spellingId, true),
      createMemoryTrackerLite(fourthNormalId, false),
    ]
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({ toRepeat: trackers })
    )
    const wrapper = await ctx.mountPage()
    type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    expect(vm.toRepeat?.map((t) => t.memoryTrackerId)).toEqual([
      normalId,
      anotherNormalId,
      spellingId,
      fourthNormalId,
    ])
    await toggleTreadmillMode(wrapper, true)
    vm.currentIndex = 1
    await wrapper.vm.$nextTick()
    await toggleTreadmillMode(wrapper, false)
    expect(vm.toRepeat?.map((t) => t.memoryTrackerId)).toEqual([
      normalId,
      anotherNormalId,
      fourthNormalId,
      spellingId,
    ])
  })
})

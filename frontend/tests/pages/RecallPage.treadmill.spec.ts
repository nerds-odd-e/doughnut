import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { MemoryTrackerLite } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { beforeEach, describe, expect, it, vi } from "vitest"
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
  let getRecallPromptSpy: ReturnType<typeof mockSdkService>

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
    getRecallPromptSpy = mockSdkService(
      MemoryTrackerController,
      "getRecallPrompt",
      makeMe.aRecallPrompt.please()
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

  it("skips spelling trackers and updates progress when treadmill is enabled", async () => {
    const wrapper = await ctx.mountPage()
    const globalBar = wrapper.findComponent({ name: "GlobalBar" })
    expect(globalBar.text()).toContain("0/3")
    await toggleTreadmillMode(wrapper, true)
    expect(globalBar.text()).toContain("0/2")
    expect(globalBar.classes()).toContain("treadmill-mode")
    expect(getRecallPromptSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { memoryTracker: normalId },
      })
    )
  })

  it("preserves currentIndex when toggling treadmill mode", async () => {
    const wrapper = await ctx.mountPage()
    type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    vm.currentIndex = 2
    await toggleTreadmillMode(wrapper, true)
    expect(vm.currentIndex).toBeGreaterThan(0)
  })

  it("moves unanswered spelling trackers to the end when treadmill is turned off", async () => {
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

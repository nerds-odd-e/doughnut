import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { MemoryTrackerLite } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
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

type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
const exposed = (wrapper: VueWrapper) => wrapper.vm as unknown as ExposedVM

const ctx = useRecallPageSpecContext({ fakeTimers: true })
let getRecallPromptSpy: ReturnType<typeof mockSdkService>

const givenQueue = (...trackers: MemoryTrackerLite[]) =>
  vi
    .mocked(useRecallData)
    .mockReturnValue(createUseRecallDataMock({ toRepeat: trackers }))

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
})

describe('RecallPage "just review" quiz', () => {
  const firstMemoryTrackerId = 123
  const secondMemoryTrackerId = 456

  beforeEach(() => {
    getRecallPromptSpy.mockResolvedValueOnce(wrapSdkError("API Error"))
    givenQueue(
      createMemoryTrackerLite(firstMemoryTrackerId),
      createMemoryTrackerLite(secondMemoryTrackerId),
      createMemoryTrackerLite(3)
    )
  })

  it("shows initial progress and asks the first tracker", async () => {
    const wrapper = await ctx.mountPage()
    expect(wrapper.findComponent({ name: "GlobalBar" }).text()).toContain("0/3")
    expect(getRecallPromptSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { memoryTracker: firstMemoryTrackerId },
      })
    )
  })

  it("advances progress after marking the current tracker as recalled", async () => {
    const wrapper = await ctx.mountPage()
    const mockedMarkAsRepeatedCall = mockSdkService(
      MemoryTrackerController,
      "markAsRecalled",
      makeMe.aMemoryTracker.please()
    )
    getRecallPromptSpy.mockResolvedValueOnce(
      wrapSdkResponse(makeMe.aRecallPrompt.please())
    )
    vi.runOnlyPendingTimers()
    await flushPromises()
    await wrapper.find("button.daisy-btn-primary").trigger("click")
    expect(mockedMarkAsRepeatedCall).toHaveBeenCalledWith({
      path: { memoryTracker: firstMemoryTrackerId },
      query: { grade: "GOOD" },
    })
    await flushPromises()
    expect(wrapper.findComponent({ name: "GlobalBar" }).text()).toContain("1/3")
    expect(getRecallPromptSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { memoryTracker: secondMemoryTrackerId },
      })
    )
  })

  it("should move current memory tracker to end when requested", async () => {
    const wrapper = await ctx.mountPage()
    const vm = exposed(wrapper)
    expect(vm.toRepeat?.map((t) => t.memoryTrackerId)).toEqual([123, 456, 3])
    await wrapper.find(".progress-bar").trigger("click")
    await wrapper.vm.$nextTick()
    await flushPromises()
    const moveToEndButton = document.body.querySelector(
      'button[title="Move to end of list"]'
    )
    expect(moveToEndButton).toBeTruthy()
    await moveToEndButton?.dispatchEvent(new Event("click"))
    await wrapper.vm.$nextTick()
    expect(vm.toRepeat?.map((t) => t.memoryTrackerId)).toEqual([456, 3, 123])
  })

  it("should not show move to end button for last item", async () => {
    const wrapper = await ctx.mountPage()
    exposed(wrapper).currentIndex = 2
    await wrapper.vm.$nextTick()
    await wrapper.find(".progress-bar").trigger("click")
    await wrapper.vm.$nextTick()
    await flushPromises()
    expect(
      document.body.querySelector('button[title="Move to end of list"]')
    ).toBeFalsy()
  })
})

describe("RecallPage treadmill mode", () => {
  const normalId = 123
  const spellingId = 456
  const anotherNormalId = 789

  const toggleTreadmillMode = async (wrapper: VueWrapper, enabled: boolean) => {
    let toggle = document.body.querySelector(
      'input[type="checkbox"]'
    ) as HTMLInputElement

    if (!toggle) {
      await wrapper.find(".progress-bar").trigger("click")
      await wrapper.vm.$nextTick()
      await flushPromises()
      await vi.waitUntil(
        () => {
          toggle = document.body.querySelector(
            'input[type="checkbox"]'
          ) as HTMLInputElement
          return !!toggle
        },
        { timeout: 1000 }
      )
    }

    toggle.checked = enabled
    toggle.dispatchEvent(new Event("change", { bubbles: true }))
    await wrapper.vm.$nextTick()
    await flushPromises()
  }

  beforeEach(() => {
    givenQueue(
      createMemoryTrackerLite(normalId, false),
      createMemoryTrackerLite(spellingId, true),
      createMemoryTrackerLite(anotherNormalId, false)
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
    exposed(wrapper).currentIndex = 2
    await toggleTreadmillMode(wrapper, true)
    expect(exposed(wrapper).currentIndex).toBeGreaterThan(0)
  })

  it("moves unanswered spelling trackers to the end when treadmill is turned off", async () => {
    const fourthNormalId = 111
    givenQueue(
      createMemoryTrackerLite(normalId, false),
      createMemoryTrackerLite(anotherNormalId, false),
      createMemoryTrackerLite(spellingId, true),
      createMemoryTrackerLite(fourthNormalId, false)
    )
    const wrapper = await ctx.mountPage()
    const vm = exposed(wrapper)
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

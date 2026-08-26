import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import type { MemoryTrackerLite } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
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

describe('RecallPage "just review" quiz', () => {
  const firstMemoryTrackerId = 123
  const secondMemoryTrackerId = 456
  const ctx = useRecallPageSpecContext({ fakeTimers: true })
  let getRecallPromptSpy: ReturnType<typeof mockSdkService>

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
    getRecallPromptSpy.mockResolvedValueOnce(wrapSdkError("API Error"))
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [
          createMemoryTrackerLite(firstMemoryTrackerId),
          createMemoryTrackerLite(secondMemoryTrackerId),
          createMemoryTrackerLite(3),
        ],
      })
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
    type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
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
    type ExposedVM = { toRepeat?: MemoryTrackerLite[]; currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    vm.currentIndex = 2
    await wrapper.vm.$nextTick()
    await wrapper.find(".progress-bar").trigger("click")
    await wrapper.vm.$nextTick()
    await flushPromises()
    expect(
      document.body.querySelector('button[title="Move to end of list"]')
    ).toBeFalsy()
  })
})

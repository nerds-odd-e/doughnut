import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
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

describe("RecallPage diligent mode", () => {
  const ctx = useRecallPageSpecContext({ fakeTimers: true })

  beforeEach(() => {
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.please()
    )
    mockSdkService(
      MemoryTrackerController,
      "askAQuestion",
      makeMe.aRecallQuestion.please()
    )
  })

  async function callLoadMore(dueInDays?: number) {
    const trackers = [createMemoryTrackerLite(123)]
    const mockData = createUseRecallDataMock({ toRepeat: trackers })
    vi.mocked(useRecallData).mockReturnValue(mockData)
    const wrapper = ctx.renderer.currentRoute({ name: "recall" }).mount()
    await flushPromises()
    ctx.recallingSpy.mockResolvedValueOnce(
      wrapSdkResponse(makeMe.aDueMemoryTrackersList.please())
    )
    type ExposedVM = { loadMore: (dueInDays?: number) => Promise<unknown> }
    await (wrapper.vm as unknown as ExposedVM).loadMore(dueInDays)
    await flushPromises()
    return mockData
  }

  it("should set diligent mode to true when loadMore is called with dueInDays > 0", async () => {
    const mockData = await callLoadMore(3)
    expect(mockData.setDiligentMode).toHaveBeenCalledWith(true)
  })

  it("should set diligent mode to false when loadMore is called with dueInDays = 0", async () => {
    const mockData = await callLoadMore(0)
    expect(mockData.setDiligentMode).toHaveBeenCalledWith(false)
  })

  it("should set diligent mode to false when loadMore is called with undefined dueInDays", async () => {
    const mockData = await callLoadMore()
    expect(mockData.setDiligentMode).toHaveBeenCalledWith(false)
  })

  it("should show red background on progress bar when in diligent mode", async () => {
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(123)],
        diligentMode: true,
      })
    )
    const wrapper = ctx.renderer.currentRoute({ name: "recall" }).mount()
    await flushPromises()
    expect(wrapper.find(".progress-bar").classes()).toContain("diligent-mode")
  })

  it("should show gray background on progress bar when not in diligent mode", async () => {
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(123)],
        diligentMode: false,
      })
    )
    const wrapper = ctx.renderer.currentRoute({ name: "recall" }).mount()
    await flushPromises()
    expect(wrapper.find(".progress-bar").classes()).not.toContain(
      "diligent-mode"
    )
  })
})

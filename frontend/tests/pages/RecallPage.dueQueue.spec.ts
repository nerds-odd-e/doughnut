import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import RecallPage from "@/pages/RecallPage.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { defineComponent, KeepAlive, nextTick } from "vue"
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

const ctx = useRecallPageSpecContext({ fakeTimers: true })

describe("RecallPage KeepAlive activation", () => {
  const menuLoadedWindowEndAt = "2026-08-27T00:00:00.123+00:00"
  const fetchedSameHalfDayWindowEndAt = "2026-08-27T00:00:00.456Z"

  const mountWithKeepAlive = () => {
    const WrapperComponent = defineComponent({
      components: { RecallPage, KeepAlive },
      data() {
        return { show: true }
      },
      template: `<KeepAlive><RecallPage v-if="show" key="recall" :eager-fetch-count="1" /></KeepAlive>`,
    })

    return helper
      .component(WrapperComponent)
      .withCleanStorage()
      .currentRoute({ name: "recall" })
      .mount()
  }

  const mountWithMenuLoadedQueue = async (fetchedWindowEndAt: string) => {
    const originalTracker = createMemoryTrackerLite(1)
    const mockData = createUseRecallDataMock({
      toRepeat: [originalTracker],
      currentRecallWindowEndAt: menuLoadedWindowEndAt,
    })
    vi.mocked(useRecallData).mockReturnValue(mockData)

    const fetchedTracker = createMemoryTrackerLite(2)
    const fetchedResponse = makeMe.aDueMemoryTrackersList
      .toRepeat([fetchedTracker])
      .please()
    fetchedResponse.currentRecallWindowEndAt = fetchedWindowEndAt
    ctx.recallingSpy.mockResolvedValue(wrapSdkResponse(fetchedResponse))

    const wrapper = mountWithKeepAlive()
    await flushPromises()
    return { wrapper, mockData, originalTracker, fetchedTracker }
  }

  const detourAndReturn = async (
    wrapper: ReturnType<typeof mountWithKeepAlive>
  ) => {
    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = false
    await nextTick()
    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = true
    await nextTick()
    await flushPromises()
  }

  it("loads the due queue on first activation when menu has not populated toRepeat", async () => {
    const loadedTracker = createMemoryTrackerLite(7)
    const loadedResponse = makeMe.aDueMemoryTrackersList
      .toRepeat([loadedTracker])
      .please()
    loadedResponse.currentRecallWindowEndAt = fetchedSameHalfDayWindowEndAt
    ctx.recallingSpy.mockResolvedValue(wrapSdkResponse(loadedResponse))
    const mockData = createUseRecallDataMock({ toRepeat: undefined })
    vi.mocked(useRecallData).mockReturnValue(mockData)

    mountWithKeepAlive()
    await flushPromises()

    expect(mockData.toRepeat.value).toEqual([loadedTracker])
  })

  it("keeps toRepeat on first activation when the due window is the same half-day", async () => {
    const { mockData, originalTracker } = await mountWithMenuLoadedQueue(
      fetchedSameHalfDayWindowEndAt
    )

    expect(ctx.recallingSpy).toHaveBeenCalled()
    expect(mockData.toRepeat.value).toEqual([originalTracker])
  })

  it("keeps the menu-loaded queue when reactivated with the same half-day due window", async () => {
    const { wrapper, mockData, originalTracker } =
      await mountWithMenuLoadedQueue(fetchedSameHalfDayWindowEndAt)

    ctx.recallingSpy.mockClear()

    await detourAndReturn(wrapper)

    expect(ctx.recallingSpy).toHaveBeenCalled()
    expect(mockData.toRepeat.value).toEqual([originalTracker])
  })

  it("remounts toRepeat when reactivated after the due window actually rolled over", async () => {
    const { wrapper, mockData, fetchedTracker } =
      await mountWithMenuLoadedQueue("2026-08-27T12:00:00.000Z")

    ctx.recallingSpy.mockClear()

    await detourAndReturn(wrapper)

    expect(ctx.recallingSpy).toHaveBeenCalled()
    expect(mockData.toRepeat.value).toEqual([fetchedTracker])
  })
})

describe("RecallPage load more buttons", () => {
  it("should show loading indicator when load more button is clicked", async () => {
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({ toRepeat: [] })
    )
    const wrapper = await ctx.mountPage()
    expect(wrapper.text()).toContain(
      "You have finished all recalls for this half a day!"
    )
    expect(wrapper.find("button.daisy-btn-secondary").exists()).toBe(true)

    let resolveRecalling: (value: unknown) => void
    const pendingPromise = new Promise((resolve) => {
      resolveRecalling = resolve
    })
    // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions
    ctx.recallingSpy.mockReturnValueOnce(pendingPromise as any)

    await wrapper.find("button.daisy-btn-secondary").trigger("click")
    await wrapper.vm.$nextTick()

    expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(true)
    expect(wrapper.text()).toContain("Loading more items...")
    expect(wrapper.find("button.daisy-btn-secondary").exists()).toBe(false)

    resolveRecalling!(wrapSdkResponse(makeMe.aDueMemoryTrackersList.please()))
    await flushPromises()
    expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(false)
  })
})

describe("RecallPage diligent mode", () => {
  beforeEach(() => {
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.please()
    )
  })

  async function callLoadMore(dueInDays?: number) {
    const mockData = createUseRecallDataMock({
      toRepeat: [createMemoryTrackerLite(123)],
    })
    vi.mocked(useRecallData).mockReturnValue(mockData)
    const wrapper = await ctx.mountPage()
    ctx.recallingSpy.mockResolvedValueOnce(
      wrapSdkResponse(makeMe.aDueMemoryTrackersList.please())
    )
    type ExposedVM = { loadMore: (dueInDays?: number) => Promise<unknown> }
    await (wrapper.vm as unknown as ExposedVM).loadMore(dueInDays)
    await flushPromises()
    return mockData
  }

  it.each([
    { dueInDays: 3, expected: true },
    { dueInDays: 0, expected: false },
    { dueInDays: undefined, expected: false },
  ])(
    "sets diligent mode to $expected when loadMore dueInDays is $dueInDays",
    async ({ dueInDays, expected }) => {
      const mockData = await callLoadMore(dueInDays)
      expect(mockData.setDiligentMode).toHaveBeenCalledWith(expected)
    }
  )

  it("shows red background on progress bar when in diligent mode", async () => {
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({
        toRepeat: [createMemoryTrackerLite(123)],
        diligentMode: true,
      })
    )
    const wrapper = await ctx.mountPage()
    expect(wrapper.find(".progress-bar").classes()).toContain("diligent-mode")
  })
})

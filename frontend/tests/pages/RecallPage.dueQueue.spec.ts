import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import RecallPage from "@/pages/RecallPage.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { defineComponent, KeepAlive, nextTick } from "vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import type { MemoryTrackerLite } from "@generated/donut-backend-api"
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

  const dueRecallsWith = (trackers: MemoryTrackerLite[]) =>
    wrapSdkResponse(makeMe.aDueMemoryTrackersList.toRepeat(trackers).please())

  it("loads the due queue on first activation when there is no queue yet", async () => {
    const loadedTracker = createMemoryTrackerLite(7)
    ctx.recallingSpy.mockResolvedValue(dueRecallsWith([loadedTracker]))
    const mockData = createUseRecallDataMock({ toRepeat: undefined })
    vi.mocked(useRecallData).mockReturnValue(mockData)

    mountWithKeepAlive()
    await flushPromises()

    expect(mockData.toRepeat.value).toEqual([loadedTracker])
  })

  it("keeps the queue and position on return and adds newly due trackers at the end", async () => {
    const tracker1 = createMemoryTrackerLite(1)
    const tracker2 = createMemoryTrackerLite(2)
    const tracker3 = createMemoryTrackerLite(3)
    const mockData = createUseRecallDataMock({ toRepeat: [tracker1, tracker2] })
    vi.mocked(useRecallData).mockReturnValue(mockData)
    ctx.recallingSpy.mockResolvedValue(dueRecallsWith([tracker1, tracker2]))
    const wrapper = mountWithKeepAlive()
    await flushPromises()
    wrapper.findComponent(RecallPage).vm.currentIndex = 1

    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = false
    await nextTick()
    ctx.recallingSpy.mockResolvedValue(dueRecallsWith([tracker2, tracker3]))
    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = true
    await flushPromises()

    expect(mockData.toRepeat.value).toEqual([tracker1, tracker2, tracker3])
    expect(mockData.currentIndex.value).toBe(1)
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

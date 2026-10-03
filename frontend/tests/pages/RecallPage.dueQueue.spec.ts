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
  givenRecallQueue,
  useRecallPageSpecContext,
} from "./recallPageTestSupport"

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
    mountWithKeepAlive()
    await flushPromises()

    expect(useRecallData().toRepeat.value).toEqual([loadedTracker])
  })

  it("keeps the queue and position on return and adds newly due trackers, including answered ones due again, at the end", async () => {
    const tracker1 = createMemoryTrackerLite(1)
    const tracker2 = createMemoryTrackerLite(2)
    const tracker3 = createMemoryTrackerLite(3)
    givenRecallQueue(tracker1, tracker2)
    ctx.recallingSpy.mockResolvedValue(dueRecallsWith([tracker1, tracker2]))
    const wrapper = mountWithKeepAlive()
    await flushPromises()
    const recallPage = wrapper.findComponent(RecallPage).vm as unknown as {
      currentIndex: number
    }
    recallPage.currentIndex = 1

    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = false
    await nextTick()
    ctx.recallingSpy.mockResolvedValue(
      dueRecallsWith([tracker1, tracker2, tracker3])
    )
    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = true
    await flushPromises()

    const { toRepeat, currentIndex } = useRecallData()
    expect(toRepeat.value).toEqual([tracker1, tracker2, tracker1, tracker3])
    expect(currentIndex.value).toBe(1)
  })
})

describe("RecallPage load more buttons", () => {
  it("should show loading indicator when load more button is clicked", async () => {
    givenRecallQueue()
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

  async function callLoadMore(
    dueInDays: number | undefined,
    initialDiligentMode: boolean
  ) {
    givenRecallQueue(createMemoryTrackerLite(123))
    useRecallData().setDiligentMode(initialDiligentMode)
    const wrapper = await ctx.mountPage()
    ctx.recallingSpy.mockResolvedValueOnce(
      wrapSdkResponse(makeMe.aDueMemoryTrackersList.please())
    )
    type ExposedVM = { loadMore: (dueInDays?: number) => Promise<unknown> }
    await (wrapper.vm as unknown as ExposedVM).loadMore(dueInDays)
    await flushPromises()
  }

  it.each([
    { dueInDays: 3, expected: true },
    { dueInDays: 0, expected: false },
    { dueInDays: undefined, expected: false },
  ])(
    "sets diligent mode to $expected when loadMore dueInDays is $dueInDays",
    async ({ dueInDays, expected }) => {
      await callLoadMore(dueInDays, !expected)
      expect(useRecallData().diligentMode.value).toBe(expected)
    }
  )

  it("shows red background on progress bar when in diligent mode", async () => {
    givenRecallQueue(createMemoryTrackerLite(123))
    useRecallData().setDiligentMode(true)
    const wrapper = await ctx.mountPage()
    expect(wrapper.find(".progress-bar").classes()).toContain("diligent-mode")
  })
})

import { useRecallData } from "@/composables/useRecallData"
import RecallPage from "@/pages/RecallPage.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { defineComponent, KeepAlive, nextTick } from "vue"
import { describe, expect, it, vi } from "vitest"
import {
  createMemoryTrackerLite,
  createUseRecallDataMock,
  mockRecallPageDefaults,
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
    const { recallingSpy } = mockRecallPageDefaults()
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
    recallingSpy.mockResolvedValue(wrapSdkResponse(fetchedResponse))

    const wrapper = mountWithKeepAlive()
    await flushPromises()
    return { wrapper, recallingSpy, mockData, originalTracker, fetchedTracker }
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

  it("keeps toRepeat on first activation when the due window is the same half-day", async () => {
    const { recallingSpy, mockData, originalTracker } =
      await mountWithMenuLoadedQueue(fetchedSameHalfDayWindowEndAt)

    expect(recallingSpy).toHaveBeenCalled()
    expect(mockData.toRepeat.value).toEqual([originalTracker])
  })

  it("keeps the menu-loaded queue when reactivated with the same half-day due window", async () => {
    const { wrapper, recallingSpy, mockData, originalTracker } =
      await mountWithMenuLoadedQueue(fetchedSameHalfDayWindowEndAt)

    recallingSpy.mockClear()

    await detourAndReturn(wrapper)

    expect(recallingSpy).toHaveBeenCalled()
    expect(mockData.toRepeat.value).toEqual([originalTracker])
  })

  it("remounts toRepeat when reactivated after the due window actually rolled over", async () => {
    const { wrapper, recallingSpy, mockData, fetchedTracker } =
      await mountWithMenuLoadedQueue("2026-08-27T12:00:00.000Z")

    recallingSpy.mockClear()

    await detourAndReturn(wrapper)

    expect(recallingSpy).toHaveBeenCalled()
    expect(mockData.toRepeat.value).toEqual([fetchedTracker])
  })
})

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

  it("does not remount menu-loaded toRepeat on first activation when the due window is the same half-day", async () => {
    const { recallingSpy } = mockRecallPageDefaults()
    const originalTracker = createMemoryTrackerLite(1)
    const mockData = createUseRecallDataMock({
      toRepeat: [originalTracker],
      currentRecallWindowEndAt: "2026-08-27T00:00:00.000Z",
    })
    vi.mocked(useRecallData).mockReturnValue(mockData)

    const sameHalfDayResponse = makeMe.aDueMemoryTrackersList
      .toRepeat([createMemoryTrackerLite(2)])
      .please()
    sameHalfDayResponse.currentRecallWindowEndAt = "2026-08-27T00:00:00.847Z"
    recallingSpy.mockResolvedValue(wrapSdkResponse(sameHalfDayResponse))

    mountWithKeepAlive()
    await flushPromises()

    expect(mockData.setToRepeat).not.toHaveBeenCalled()
    expect(mockData.toRepeat.value).toEqual([originalTracker])
  })

  it("does not remount toRepeat when reactivated with an unchanged due window", async () => {
    const { recallingSpy } = mockRecallPageDefaults()
    const unchangedWindowEndAt = "2026-08-27T00:00:00.000Z"
    const originalTracker = createMemoryTrackerLite(1)
    const mockData = createUseRecallDataMock({
      toRepeat: [originalTracker],
      currentRecallWindowEndAt: unchangedWindowEndAt,
    })
    vi.mocked(useRecallData).mockReturnValue(mockData)

    const staleWindowResponse = makeMe.aDueMemoryTrackersList
      .toRepeat([createMemoryTrackerLite(2)])
      .please()
    // Same half-day as the stored window, but leftover millis like production
    // `alignByHalfADay` used to emit — exact string equality would remount.
    staleWindowResponse.currentRecallWindowEndAt = "2026-08-27T00:00:00.847Z"
    recallingSpy.mockResolvedValue(wrapSdkResponse(staleWindowResponse))

    const wrapper = mountWithKeepAlive()
    await flushPromises()

    recallingSpy.mockClear()
    mockData.setToRepeat.mockClear()

    // Detour away (deactivate) and return (activate) via KeepAlive.
    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = false
    await nextTick()
    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = true
    await nextTick()
    await flushPromises()

    expect(recallingSpy).toHaveBeenCalled()
    expect(mockData.setToRepeat).not.toHaveBeenCalled()
    expect(mockData.toRepeat.value).toEqual([originalTracker])
  })

  it("remounts toRepeat when reactivated after the due window actually rolled over", async () => {
    const { recallingSpy } = mockRecallPageDefaults()
    const originalTracker = createMemoryTrackerLite(1)
    const rolledOverTracker = createMemoryTrackerLite(2)
    const mockData = createUseRecallDataMock({
      toRepeat: [originalTracker],
      currentRecallWindowEndAt: "2026-08-27T00:00:00.000Z",
    })
    vi.mocked(useRecallData).mockReturnValue(mockData)

    const rolledOverResponse = makeMe.aDueMemoryTrackersList
      .toRepeat([rolledOverTracker])
      .please()
    rolledOverResponse.currentRecallWindowEndAt = "2026-08-27T12:00:00.000Z"
    recallingSpy.mockResolvedValue(wrapSdkResponse(rolledOverResponse))

    const wrapper = mountWithKeepAlive()
    await flushPromises()

    recallingSpy.mockClear()
    mockData.setToRepeat.mockClear()

    // Detour away (deactivate) and return (activate) via KeepAlive.
    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = false
    await nextTick()
    // biome-ignore lint/suspicious/noExplicitAny: test wrapper's own data property
    ;(wrapper.vm as any).show = true
    await nextTick()
    await flushPromises()

    expect(recallingSpy).toHaveBeenCalled()
    expect(mockData.setToRepeat).toHaveBeenCalled()
    expect(mockData.toRepeat.value).toEqual([rolledOverTracker])
  })
})

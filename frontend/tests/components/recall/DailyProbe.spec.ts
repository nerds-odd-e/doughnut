import DailyProbe from "@/components/recall/DailyProbe.vue"
import {
  DAILY_PROBE_INSTRUCTION,
  DAILY_PROBE_ISI_MS,
  DAILY_PROBE_TIMEOUT_MS,
  dailyProbeRunSequence,
} from "@/models/dailyProbe"
import { DailyProbeController } from "@generated/donut-backend-api/sdk.gen"
import helper, {
  mockSdkService,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { defineComponent, KeepAlive, nextTick } from "vue"
import {
  completeProbe,
  expectBlankStimulusSlot,
  mountDailyProbe,
  pressMappedKey,
  responseZone,
  stimulusSlot,
  tapMappedSide,
} from "./dailyProbeTestSupport"

describe("DailyProbe", () => {
  let wrapper: VueWrapper | undefined
  let createDailyProbe: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    vi.useFakeTimers()
    createDailyProbe = mockSdkService(
      DailyProbeController,
      "createDailyProbe",
      {
        id: 1,
      }
    )
    createDailyProbe.mockClear()
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
    vi.useRealTimers()
    document.body.innerHTML = ""
  })

  const mountProbe = () => {
    wrapper = mountDailyProbe()
    return wrapper
  }

  it("shows the protocol instruction", () => {
    expect(mountProbe().text()).toContain(DAILY_PROBE_INSTRUCTION)
  })

  it("shows unlabeled side zones", async () => {
    const view = mountProbe()
    await nextTick()
    const left = responseZone(view, "left")
    const right = responseZone(view, "right")
    expect(left.classes()).toContain("bg-base-200")
    expect(right.classes()).toContain("bg-base-200")
    expect(left.element.parentElement?.classList.contains("divide-x")).toBe(
      true
    )
    expect(left.text()).not.toMatch(/[←→]|Left|Right|[FfJj]/)
    expect(right.text()).not.toMatch(/[←→]|Left|Right|[FfJj]/)
  })

  it("keeps the instruction still and fills remaining height with tap panels", async () => {
    const Host = defineComponent({
      components: { DailyProbe },
      template: `<div style="height: 640px; width: 360px; display: flex; flex-direction: column"><DailyProbe /></div>`,
    })
    wrapper = helper.component(Host).mount({ attachTo: document.body })
    await nextTick()

    const instruction = wrapper.find(
      '[data-testid="daily-probe-instruction"]'
    ).element
    const slot = stimulusSlot(wrapper)
    const instructionTop = instruction.getBoundingClientRect().top
    const slotHeight = slot.element.getBoundingClientRect().height
    expect(slot.text()).toMatch(/[←→]/)

    const zoneHeight = responseZone(
      wrapper,
      "left"
    ).element.getBoundingClientRect().height
    const remaining =
      wrapper.element.getBoundingClientRect().bottom -
      slot.element.getBoundingClientRect().bottom
    expect(zoneHeight).toBeGreaterThan(96)
    expect(zoneHeight).toBeGreaterThan(remaining * 0.5)

    await tapMappedSide(wrapper, "left")
    await nextTick()

    expect(instruction.getBoundingClientRect().top).toBe(instructionTop)
    expectBlankStimulusSlot(wrapper)
    expect(slot.element.getBoundingClientRect().height).toBe(slotHeight)
  })

  it("flashes the left zone after a left tap and clears after 200ms", async () => {
    const view = mountProbe()
    await tapMappedSide(view, "left")
    await nextTick()
    expect(responseZone(view, "left").classes()).toContain("bg-base-300")
    expect(responseZone(view, "right").classes()).not.toContain("bg-base-300")
    expectBlankStimulusSlot(view)

    vi.advanceTimersByTime(200)
    await nextTick()
    expect(responseZone(view, "left").classes()).not.toContain("bg-base-300")
  })

  it("flashes the left zone after F", async () => {
    const view = mountProbe()
    pressMappedKey("left")
    await nextTick()
    expect(responseZone(view, "left").classes()).toContain("bg-base-300")
  })

  it("does not flash when a trial times out", async () => {
    const view = mountProbe()
    vi.advanceTimersByTime(DAILY_PROBE_TIMEOUT_MS)
    await nextTick()
    expect(responseZone(view, "left").classes()).not.toContain("bg-base-300")
    expect(responseZone(view, "right").classes()).not.toContain("bg-base-300")
  })

  it("records a matching side-zone tap the same as F/J", async () => {
    const view = mountProbe()
    await completeProbe((side) => tapMappedSide(view, side))
    expect(view.text()).toContain("4.00")
  })

  it("ignores a second tap and taps during the blank ISI", async () => {
    const view = mountProbe()
    await tapMappedSide(view, "left")
    expectBlankStimulusSlot(view)

    await tapMappedSide(view, "left")
    await tapMappedSide(view, "right")
    expectBlankStimulusSlot(view)

    vi.advanceTimersByTime(DAILY_PROBE_ISI_MS)
    await nextTick()
    expect(stimulusSlot(view).text()).toBe("→")
  })

  it("shows speed 4.00 after every correct mapped key at 250 ms", async () => {
    const view = mountProbe()
    await completeProbe(pressMappedKey)
    expect(view.text()).toContain("4.00")
    expect(view.text()).toContain("Continue")
  })

  it("shows Saved after posting twenty scored trials", async () => {
    const view = mountProbe()
    await completeProbe(pressMappedKey)
    expect(view.find('[data-testid="daily-probe-saved"]').text()).toBe("Saved")
    expect(createDailyProbe).toHaveBeenCalledTimes(1)
    const posted = createDailyProbe.mock.calls as [
      [{ body: { trials: unknown[] } }],
    ]
    expect(posted[0][0].body.trials).toHaveLength(20)
  })

  it("keeps Continue disabled until Saved", async () => {
    let resolveSave!: () => void
    createDailyProbe.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveSave = () => resolve(wrapSdkResponse({ id: 1 }))
        })
    )
    const view = mountProbe()
    await completeProbe(pressMappedKey)
    const continueButton = view.find('[data-testid="daily-probe-continue"]')
    expect(continueButton.attributes("disabled")).toBeDefined()
    expect(view.find('[data-testid="daily-probe-saved"]').exists()).toBe(false)

    resolveSave()
    await flushPromises()
    expect(continueButton.attributes("disabled")).toBeUndefined()
  })

  it("shows retry after a failed save and does not emit complete", async () => {
    createDailyProbe.mockResolvedValue(wrapSdkError("save failed"))
    const view = mountProbe()
    await completeProbe(pressMappedKey)
    expect(view.find('[data-testid="daily-probe-retry"]').exists()).toBe(true)
    const continueButton = view.find('[data-testid="daily-probe-continue"]')
    ;(continueButton.element as HTMLButtonElement).click()
    await flushPromises()
    expect(view.emitted("complete")).toBeUndefined()
  })

  it("posts again when retrying a failed save", async () => {
    createDailyProbe.mockResolvedValueOnce(wrapSdkError("save failed"))
    const view = mountProbe()
    await completeProbe(pressMappedKey)
    expect(createDailyProbe).toHaveBeenCalledTimes(1)

    await view.find('[data-testid="daily-probe-retry"]').trigger("click")
    await flushPromises()
    expect(createDailyProbe).toHaveBeenCalledTimes(2)
    expect(view.find('[data-testid="daily-probe-saved"]').text()).toBe("Saved")
  })

  it("does not post and restarts after a KeepAlive detour mid-run", async () => {
    const WrapperComponent = defineComponent({
      components: { DailyProbe, KeepAlive },
      data() {
        return { show: true }
      },
      template: `<KeepAlive><DailyProbe v-if="show" key="daily-probe" /></KeepAlive>`,
    })
    wrapper = helper.component(WrapperComponent).mount()
    await nextTick()
    expect(stimulusSlot(wrapper).text()).toBe("←")

    vi.advanceTimersByTime(DAILY_PROBE_TIMEOUT_MS + DAILY_PROBE_ISI_MS)
    await nextTick()
    expect(stimulusSlot(wrapper).text()).toBe("→")

    await wrapper.setData({ show: false })
    await nextTick()
    const trialCount = dailyProbeRunSequence.length
    vi.advanceTimersByTime(
      trialCount * (DAILY_PROBE_TIMEOUT_MS + DAILY_PROBE_ISI_MS)
    )
    await flushPromises()
    expect(createDailyProbe).not.toHaveBeenCalled()

    await wrapper.setData({ show: true })
    await nextTick()
    expect(stimulusSlot(wrapper).text()).toBe("←")
  })
})

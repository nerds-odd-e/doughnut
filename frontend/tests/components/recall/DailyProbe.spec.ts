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

function pressMappedKey(side: "left" | "right") {
  window.dispatchEvent(
    new KeyboardEvent("keydown", {
      key: side === "left" ? "f" : "j",
      bubbles: true,
    })
  )
}

function tapMappedSide(view: VueWrapper, side: "left" | "right") {
  return view
    .find(`[data-testid="daily-probe-response-zone-${side}"]`)
    .trigger("pointerdown")
}

async function completeProbe(respond: (side: "left" | "right") => unknown) {
  for (const side of dailyProbeRunSequence) {
    vi.advanceTimersByTime(250)
    await respond(side)
    vi.advanceTimersByTime(DAILY_PROBE_ISI_MS)
  }
  await flushPromises()
}

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
    wrapper = helper.component(DailyProbe).mount()
    return wrapper
  }

  it("shows the protocol instruction", () => {
    expect(mountProbe().text()).toContain(DAILY_PROBE_INSTRUCTION)
  })

  it("shows unlabeled side zones while the stimulus uses arrows", async () => {
    const view = mountProbe()
    await nextTick()
    const left = view.find('[data-testid="daily-probe-response-zone-left"]')
    const right = view.find('[data-testid="daily-probe-response-zone-right"]')
    expect(left.text()).not.toMatch(/[←→]/)
    expect(right.text()).not.toMatch(/[←→]/)
    expect(view.find('[data-testid="daily-probe-stimulus"]').text()).toBe("←")
  })

  it("records a matching side-zone tap the same as F/J", async () => {
    const view = mountProbe()
    await completeProbe((side) => tapMappedSide(view, side))
    expect(view.text()).toContain("4.00")
  })

  it("ignores a second tap and taps during the blank ISI", async () => {
    const view = mountProbe()
    await tapMappedSide(view, "left")
    expect(view.find('[data-testid="daily-probe-stimulus"]').exists()).toBe(
      false
    )

    await tapMappedSide(view, "left")
    await tapMappedSide(view, "right")
    expect(view.find('[data-testid="daily-probe-stimulus"]').exists()).toBe(
      false
    )

    vi.advanceTimersByTime(DAILY_PROBE_ISI_MS)
    await nextTick()
    expect(view.find('[data-testid="daily-probe-stimulus"]').text()).toBe("→")
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
    expect(wrapper.find('[data-testid="daily-probe-stimulus"]').text()).toBe(
      "←"
    )

    vi.advanceTimersByTime(DAILY_PROBE_TIMEOUT_MS + DAILY_PROBE_ISI_MS)
    await nextTick()
    expect(wrapper.find('[data-testid="daily-probe-stimulus"]').text()).toBe(
      "→"
    )

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
    expect(wrapper.find('[data-testid="daily-probe-stimulus"]').text()).toBe(
      "←"
    )
  })
})

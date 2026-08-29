import DailyProbe from "@/components/recall/DailyProbe.vue"
import {
  DAILY_PROBE_INSTRUCTION,
  DAILY_PROBE_ISI_MS,
  DAILY_PROBE_TIMEOUT_MS,
  dailyProbePracticeSequence,
  dailyProbeScoredSequence,
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

async function completeProbeWithMappedKeys() {
  const sequence = [...dailyProbePracticeSequence, ...dailyProbeScoredSequence]
  for (const side of sequence) {
    vi.advanceTimersByTime(250)
    pressMappedKey(side)
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

  it("shows speed 4.00 after every correct mapped key at 250 ms", async () => {
    const view = mountProbe()
    await completeProbeWithMappedKeys()
    expect(view.text()).toContain("4.00")
    expect(view.text()).toContain("Continue")
  })

  it("shows Saved after posting twenty scored trials", async () => {
    const view = mountProbe()
    await completeProbeWithMappedKeys()
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
    await completeProbeWithMappedKeys()
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
    await completeProbeWithMappedKeys()
    expect(view.find('[data-testid="daily-probe-retry"]').exists()).toBe(true)
    const continueButton = view.find('[data-testid="daily-probe-continue"]')
    ;(continueButton.element as HTMLButtonElement).click()
    await flushPromises()
    expect(view.emitted("complete")).toBeUndefined()
  })

  it("posts again when retrying a failed save", async () => {
    createDailyProbe.mockResolvedValueOnce(wrapSdkError("save failed"))
    const view = mountProbe()
    await completeProbeWithMappedKeys()
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
    const trialCount =
      dailyProbePracticeSequence.length + dailyProbeScoredSequence.length
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

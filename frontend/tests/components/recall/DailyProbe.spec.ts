import DailyProbe from "@/components/recall/DailyProbe.vue"
import {
  DAILY_PROBE_ISI_MS,
  dailyProbePracticeSequence,
  dailyProbeScoredSequence,
} from "@/models/dailyProbe"
import { DailyProbeController } from "@generated/donut-backend-api/sdk.gen"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

const instruction =
  "Each trial shows ← or →. Press F for left, J for right (arrow keys also work). Go as fast as you can without mistakes."

function pressMappedKey(side: "left" | "right") {
  window.dispatchEvent(
    new KeyboardEvent("keydown", {
      key: side === "left" ? "f" : "j",
      bubbles: true,
    })
  )
}

async function completeProbeWithMappedKeys({
  wrongScored = () => false,
  rtMsForScoredIndex = () => 250,
}: {
  wrongScored?: (scoredIndex: number) => boolean
  rtMsForScoredIndex?: (scoredIndex: number) => number
} = {}) {
  const practiceCount = dailyProbePracticeSequence.length
  const sequence = [...dailyProbePracticeSequence, ...dailyProbeScoredSequence]
  for (const [index, side] of sequence.entries()) {
    const scoredIndex = index - practiceCount
    vi.advanceTimersByTime(
      scoredIndex < 0 ? 250 : rtMsForScoredIndex(scoredIndex)
    )
    const wrong = wrongScored(scoredIndex)
    pressMappedKey(wrong ? (side === "left" ? "right" : "left") : side)
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
    expect(mountProbe().text()).toContain(instruction)
  })

  it("shows speed 4.00 after every correct mapped key at 250 ms", async () => {
    const view = mountProbe()
    await completeProbeWithMappedKeys()
    expect(view.text()).toContain("4.00")
    expect(view.text()).toContain("Continue")
  })

  it("shows accuracy 95% after one wrong scored key", async () => {
    const view = mountProbe()
    await completeProbeWithMappedKeys({
      wrongScored: (scoredIndex) => scoredIndex === 0,
    })
    expect(view.find('[data-testid="daily-probe-accuracy"]').text()).toContain(
      "95%"
    )
  })

  it("shows lapse count 1 after one 500 ms scored trial among 250 ms trials", async () => {
    const view = mountProbe()
    await completeProbeWithMappedKeys({
      rtMsForScoredIndex: (scoredIndex) => (scoredIndex === 0 ? 500 : 250),
    })
    expect(view.find('[data-testid="daily-probe-lapses"]').text()).toContain(
      "1"
    )
  })

  it("shows variability 1.41 after correct 250 ms and 500 ms scored trials", async () => {
    const view = mountProbe()
    await completeProbeWithMappedKeys({
      wrongScored: (scoredIndex) => scoredIndex >= 2,
      rtMsForScoredIndex: (scoredIndex) => (scoredIndex === 1 ? 500 : 250),
    })
    expect(
      view.find('[data-testid="daily-probe-variability"]').text()
    ).toContain("1.41")
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
})

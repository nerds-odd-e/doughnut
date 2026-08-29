import DailyProbe from "@/components/recall/DailyProbe.vue"
import {
  DAILY_PROBE_ISI_MS,
  dailyProbePracticeSequence,
  dailyProbeScoredSequence,
} from "@/models/dailyProbe"
import helper from "@tests/helpers"
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

  beforeEach(() => {
    vi.useFakeTimers()
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
})

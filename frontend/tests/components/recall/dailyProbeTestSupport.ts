import DailyProbe from "@/components/recall/DailyProbe.vue"
import { DAILY_PROBE_ISI_MS, dailyProbeRunSequence } from "@/models/dailyProbe"
import helper from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { expect, vi } from "vitest"

export function pressMappedKey(side: "left" | "right") {
  window.dispatchEvent(
    new KeyboardEvent("keydown", {
      key: side === "left" ? "f" : "j",
      bubbles: true,
    })
  )
}

export function responseZone(view: VueWrapper, side: "left" | "right") {
  return view.find(`[data-testid="daily-probe-response-zone-${side}"]`)
}

export function tapMappedSide(view: VueWrapper, side: "left" | "right") {
  return responseZone(view, side).trigger("pointerdown")
}

export function stimulusSlot(view: VueWrapper) {
  return view.find('[data-testid="daily-probe-stimulus"]')
}

export function expectBlankStimulusSlot(view: VueWrapper) {
  const slot = stimulusSlot(view)
  expect(slot.exists()).toBe(true)
  expect(slot.classes()).toContain("invisible")
  expect(slot.text()).not.toMatch(/[←→]/)
}

export async function completeProbe(
  respond: (side: "left" | "right") => unknown
) {
  for (const side of dailyProbeRunSequence) {
    vi.advanceTimersByTime(250)
    await respond(side)
    vi.advanceTimersByTime(DAILY_PROBE_ISI_MS)
  }
  await flushPromises()
}

export function mountDailyProbe() {
  return helper.component(DailyProbe).mount()
}

import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"

/** Advance one animation frame under fake or real timers. */
export async function advanceAnimationFrame() {
  if (vi.isFakeTimers()) {
    await vi.advanceTimersToNextFrame()
    return
  }
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
}

/** Wait for `scheduleFocusTargetWithin` (nextTick + requestAnimationFrame). */
export async function settleScheduledAutofocus() {
  await flushPromises()
  await advanceAnimationFrame()
}

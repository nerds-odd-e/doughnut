import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"
import { nextTick } from "vue"

/** Advance one animation frame under fake or real timers. */
export async function advanceAnimationFrame() {
  if (vi.isFakeTimers()) {
    await vi.advanceTimersToNextFrame()
    return
  }
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
}

/** Settle Vue's DOM update and scheduled autofocus. */
export async function settleScheduledAutofocus() {
  await flushPromises()
  await nextTick()
}

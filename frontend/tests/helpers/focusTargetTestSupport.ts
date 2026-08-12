import { flushPromises } from "@vue/test-utils"

/** Wait for `scheduleFocusTargetWithin` (nextTick + requestAnimationFrame). */
export async function settleScheduledAutofocus() {
  await flushPromises()
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
}

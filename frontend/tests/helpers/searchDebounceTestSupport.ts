import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { vi } from "vitest"

/** Matches `useSearchExecution` debounce interval. */
export const SEARCH_DEBOUNCE_MS = 1000

export async function advanceSearchDebounce() {
  await nextTick()
  vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS)
  await flushPromises()
}

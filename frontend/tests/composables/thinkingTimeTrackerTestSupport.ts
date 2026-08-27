import { afterEach, beforeEach, expect, vi } from "vitest"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { nextTick, type Component } from "vue"
import type { Clock } from "@/composables/useThinkingTimeTracker"

export const flushStart = async () => {
  await nextTick()
  await flushPromises()
}

export const mountAndFlush = async (component: Component) => {
  const wrapper = mount(component)
  await flushStart()
  return wrapper
}

// Fake timers + an injectable clock so tests can move the tracker's clock in
// discrete steps, plus a synchronous requestAnimationFrame stub so start()
// resolves without waiting a real frame.
export const setupTrackerClock = () => {
  let currentMs = 0
  const clock: Clock = { now: () => currentMs }

  beforeEach(() => {
    currentMs = 0
    vi.useFakeTimers()
    vi.stubGlobal("requestAnimationFrame", (callback: FrameRequestCallback) => {
      callback(0)
      return 1
    })
  })

  afterEach(() => {
    Object.defineProperty(document, "hidden", { value: false, writable: true })
    vi.unstubAllGlobals()
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  const setTime = (ms: number) => {
    currentMs = ms
    vi.advanceTimersByTime(ms)
  }

  // Moves only the clock, without advancing fake timers — for asserting
  // behavior that must be reconciled before any timer (e.g. the watchdog
  // interval) has a chance to fire.
  const mockNow = (ms: number) => {
    currentMs = ms
  }

  return { clock, setTime, mockNow }
}

export const stopAndExpect = async (wrapper: VueWrapper, expected: string) => {
  await wrapper.get('[data-testid="stop"]').trigger("click")
  expect(wrapper.get('[data-testid="result"]').text()).toBe(expected)
}

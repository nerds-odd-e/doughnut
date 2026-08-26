import { afterEach, beforeEach, expect, vi } from "vitest"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { nextTick, type Component } from "vue"

export const flushStart = async () => {
  await nextTick()
  await flushPromises()
}

export const mountAndFlush = async (component: Component) => {
  const wrapper = mount(component)
  await flushStart()
  return wrapper
}

// Fake timers + a controllable performance.now() so tests can move the
// tracker's clock in discrete steps, plus a synchronous requestAnimationFrame
// stub so start() resolves without waiting a real frame.
export const setupTrackerClock = () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
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
    performanceNowSpy.mockReturnValue(ms)
    vi.advanceTimersByTime(ms)
  }

  // Moves only performance.now(), without advancing fake timers — for
  // asserting behavior that must be reconciled before any timer (e.g. the
  // watchdog interval) has a chance to fire.
  const mockNow = (ms: number) => {
    performanceNowSpy.mockReturnValue(ms)
  }

  return { setTime, mockNow }
}

export const stopAndExpect = async (wrapper: VueWrapper, expected: string) => {
  await wrapper.get('[data-testid="stop"]').trigger("click")
  expect(wrapper.get('[data-testid="result"]').text()).toBe(expected)
}

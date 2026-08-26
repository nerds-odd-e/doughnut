import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { flushPromises, mount } from "@vue/test-utils"
import { defineComponent, ref, nextTick } from "vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"

describe("useThinkingTimeTracker", () => {
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

  const flushStart = async () => {
    await nextTick()
    await flushPromises()
  }

  const createStartedTrackerComponent = () =>
    defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        const result = ref<number | null>(null)

        start()

        const handleStop = () => {
          result.value = stop()
        }

        return { handleStop, result }
      },
      template: `
        <div>
          <button data-testid="stop" @click="handleStop">Stop</button>
          <span data-testid="result">{{ result }}</span>
        </div>
      `,
    })

  const setTime = (ms: number) => {
    performanceNowSpy.mockReturnValue(ms)
    vi.advanceTimersByTime(ms)
  }

  const mountStartedTracker = async () => {
    const wrapper = mount(createStartedTrackerComponent())
    await flushStart()
    return wrapper
  }

  const stopAndExpect = async (
    wrapper: ReturnType<typeof mount>,
    expected: string
  ) => {
    await wrapper.get('[data-testid="stop"]').trigger("click")
    expect(wrapper.get('[data-testid="result"]').text()).toBe(expected)
  }

  it("starts timer after nextTick and requestAnimationFrame", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)
    await stopAndExpect(wrapper, "1000")
  })

  it("pauses when page becomes hidden", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    Object.defineProperty(document, "hidden", { value: true, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    setTime(2000)
    await stopAndExpect(wrapper, "1000")
  })

  it("resumes when page becomes visible", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    Object.defineProperty(document, "hidden", { value: true, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    setTime(2000)

    Object.defineProperty(document, "hidden", { value: false, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    setTime(3000)
    await stopAndExpect(wrapper, "2000")
  })

  it("does not resume on focus while document is hidden", async () => {
    const wrapper = await mountStartedTracker()
    setTime(500)

    Object.defineProperty(document, "hidden", { value: true, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    setTime(2000)

    window.dispatchEvent(new Event("focus"))
    setTime(3000)

    await stopAndExpect(wrapper, "500")
  })

  it("pauses via visibility sync when hidden without earlier events", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    Object.defineProperty(document, "hidden", { value: true, writable: true })

    vi.advanceTimersByTime(300)

    setTime(2000)
    await stopAndExpect(wrapper, "1000")
  })

  it("only records once per stop call", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    await stopAndExpect(wrapper, "1000")
    await stopAndExpect(wrapper, "1000")
  })

  it("returns rounded milliseconds", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1234.567)
    await stopAndExpect(wrapper, "1235")
  })

  it("excludes a suspend gap reconciled by the watchdog tick, with no pause() called", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    // Device suspends without firing any pause/resume event. The clock
    // jumps forward by hours in a single tick — this is what a watchdog
    // tick observes once the interval fires again after wake.
    setTime(1000 + 6 * 60 * 60 * 1000)

    setTime(1000 + 6 * 60 * 60 * 1000 + 500)
    await stopAndExpect(wrapper, "1500")
  })

  it("excludes a suspend gap reconciled in stop() before any watchdog tick fires", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    // Jump the clock only — do not advance fake timers, so the 250ms
    // watchdog interval has no chance to tick before stop() is called.
    performanceNowSpy.mockReturnValue(1000 + 6 * 60 * 60 * 1000)

    await stopAndExpect(wrapper, "1000")
  })

  it("does not exclude ordinary small clock drift", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)
    setTime(4000)
    await stopAndExpect(wrapper, "4000")
  })
})

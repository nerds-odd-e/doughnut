import { describe, it, expect } from "vitest"
import { defineComponent, ref, nextTick } from "vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"
import {
  mountAndFlush,
  setupTrackerClock,
  stopAndExpect,
} from "./thinkingTimeTrackerTestSupport"

describe("useThinkingTimeTracker idle detection", () => {
  const { clock, setTime, mockNow } = setupTrackerClock()

  const createStartedTrackerComponent = () =>
    defineComponent({
      setup() {
        const { start, stop, idleMs } = useThinkingTimeTracker({ clock })
        const result = ref<number | null>(null)

        start()

        const handleStop = () => {
          result.value = stop()
        }

        return { handleStop, result, idleMs }
      },
      template: `
        <div>
          <button data-testid="stop" @click="handleStop">Stop</button>
          <span data-testid="result">{{ result }}</span>
          <span data-testid="idle-ms">{{ idleMs }}</span>
        </div>
      `,
    })

  // Advances the mocked clock in <5000ms steps so each jump stays under
  // SUSPEND_GAP_THRESHOLD_MS and is included in thinking time, unlike a
  // single large jump (which the tracker treats as a device suspend).
  const advanceTimeTo = (currentMs: number, targetMs: number): number => {
    let time = currentMs
    while (time < targetMs) {
      time = Math.min(time + 4000, targetMs)
      setTime(time)
    }
    return time
  }

  const mountStartedTracker = () =>
    mountAndFlush(createStartedTrackerComponent())

  it("does not flag idle time when the inactivity stretch stays under the threshold", async () => {
    const wrapper = await mountStartedTracker()

    advanceTimeTo(0, 30000)
    await nextTick()

    expect(wrapper.get('[data-testid="idle-ms"]').text()).toBe("0")
  })

  it("flags only the portion of an inactivity stretch beyond the threshold, keeping it inside thinking time", async () => {
    const wrapper = await mountStartedTracker()

    advanceTimeTo(0, 70000)
    await nextTick()

    expect(wrapper.get('[data-testid="idle-ms"]').text()).toBe("10000")

    await stopAndExpect(wrapper, "70000")
  })

  it("resets idle detection on activity without retroactively removing already-counted idle time", async () => {
    const wrapper = await mountStartedTracker()

    let time = advanceTimeTo(0, 70000)
    await nextTick()
    expect(wrapper.get('[data-testid="idle-ms"]').text()).toBe("10000")

    window.dispatchEvent(new Event("mousemove"))
    time = advanceTimeTo(time, 70500)
    await nextTick()

    expect(wrapper.get('[data-testid="idle-ms"]').text()).toBe("10000")
  })

  it("excludes a silent device suspend gap from idle time", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    // Device suspends without firing any pause/resume event and without any
    // recorded activity beforehand. If the idle detector's activity
    // baseline weren't rebased when reconcileGap() drops this gap, the next
    // watchdog tick would attribute the whole sleep duration to idle time.
    setTime(1000 + 6 * 60 * 60 * 1000)
    await nextTick()

    expect(wrapper.get('[data-testid="idle-ms"]').text()).toBe("0")

    setTime(1000 + 6 * 60 * 60 * 1000 + 500)
    await stopAndExpect(wrapper, "1500")
    expect(wrapper.get('[data-testid="idle-ms"]').text()).toBe("0")
  })

  it("flushes the in-progress idle stretch on stop() without waiting for the watchdog", async () => {
    const wrapper = await mountStartedTracker()

    advanceTimeTo(0, 69500)
    await nextTick()
    expect(wrapper.get('[data-testid="idle-ms"]').text()).toBe("9500")

    // Move the clock only — no watchdog tick fires before stop() is called.
    mockNow(70000)
    await stopAndExpect(wrapper, "70000")

    expect(wrapper.get('[data-testid="idle-ms"]').text()).toBe("10000")
  })
})

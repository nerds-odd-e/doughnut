import { describe, it, expect } from "vitest"
import { defineComponent, ref, nextTick } from "vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"
import {
  mountAndFlush,
  setupTrackerClock,
  stopAndExpect,
} from "./thinkingTimeTrackerTestSupport"

describe("useThinkingTimeTracker interruption accumulators", () => {
  const { clock, setTime } = setupTrackerClock()

  const createStartedTrackerComponent = () =>
    defineComponent({
      setup() {
        const {
          start,
          stop,
          pause,
          resume,
          pauseForDetour,
          resumeFromDetour,
          awayMs,
          awayCount,
          detourMs,
          detourCount,
        } = useThinkingTimeTracker({ clock })
        const result = ref<number | null>(null)

        start()

        const handleStop = () => {
          result.value = stop()
        }

        return {
          handleStop,
          result,
          pause,
          resume,
          pauseForDetour,
          resumeFromDetour,
          awayMs,
          awayCount,
          detourMs,
          detourCount,
        }
      },
      template: `
        <div>
          <button data-testid="stop" @click="handleStop">Stop</button>
          <button data-testid="pause" @click="pause">Pause</button>
          <button data-testid="resume" @click="resume">Resume</button>
          <button data-testid="pause-detour" @click="pauseForDetour">Pause detour</button>
          <button data-testid="resume-detour" @click="resumeFromDetour">Resume detour</button>
          <span data-testid="result">{{ result }}</span>
          <span data-testid="away-ms">{{ awayMs }}</span>
          <span data-testid="away-count">{{ awayCount }}</span>
          <span data-testid="detour-ms">{{ detourMs }}</span>
          <span data-testid="detour-count">{{ detourCount }}</span>
        </div>
      `,
    })

  const mountStartedTracker = () =>
    mountAndFlush(createStartedTrackerComponent())

  it("records away time and count when the page becomes hidden and visible again", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    Object.defineProperty(document, "hidden", { value: true, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    setTime(2500)

    Object.defineProperty(document, "hidden", { value: false, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    setTime(3000)
    await nextTick()

    expect(wrapper.get('[data-testid="away-ms"]').text()).toBe("1500")
    expect(wrapper.get('[data-testid="away-count"]').text()).toBe("1")
  })

  it("records detour time and count when paused and resumed for a detour", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    await wrapper.get('[data-testid="pause-detour"]').trigger("click")
    setTime(2500)
    await wrapper.get('[data-testid="resume-detour"]').trigger("click")

    setTime(3000)
    await nextTick()

    expect(wrapper.get('[data-testid="detour-ms"]').text()).toBe("1500")
    expect(wrapper.get('[data-testid="detour-count"]').text()).toBe("1")
    await stopAndExpect(wrapper, "1500")
  })

  it("does not count plain pause/resume as detour time", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    await wrapper.get('[data-testid="pause"]').trigger("click")
    setTime(2500)
    await wrapper.get('[data-testid="resume"]').trigger("click")

    setTime(3000)
    await nextTick()

    expect(wrapper.get('[data-testid="detour-ms"]').text()).toBe("0")
    expect(wrapper.get('[data-testid="detour-count"]').text()).toBe("0")
  })

  it("does not count a detour pause/resume as away time", async () => {
    const wrapper = await mountStartedTracker()
    setTime(1000)

    await wrapper.get('[data-testid="pause-detour"]').trigger("click")
    setTime(2500)
    await wrapper.get('[data-testid="resume-detour"]').trigger("click")

    setTime(3000)
    await nextTick()

    expect(wrapper.get('[data-testid="away-ms"]').text()).toBe("0")
    expect(wrapper.get('[data-testid="away-count"]').text()).toBe("0")
  })
})

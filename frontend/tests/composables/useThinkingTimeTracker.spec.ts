import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { flushPromises, mount } from "@vue/test-utils"
import {
  defineComponent,
  ref,
  KeepAlive,
  onActivated,
  onDeactivated,
  nextTick,
} from "vue"
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

  describe("KeepAlive lifecycle", () => {
    const InnerComponent = defineComponent({
      setup() {
        const { start, stop, pause, resume } = useThinkingTimeTracker()
        const result = ref<number | null>(null)

        onActivated(() => {
          start()
          resume()
        })
        onDeactivated(() => pause())

        start()

        const handleStop = () => {
          result.value = stop()
        }

        return { handleStop, result }
      },
      template: `
        <div>
          <button data-testid="inner-stop" @click="handleStop">Stop</button>
          <span data-testid="inner-result">{{ result }}</span>
        </div>
      `,
    })

    const WrapperComponent = defineComponent({
      components: { InnerComponent, KeepAlive },
      setup() {
        const show = ref(true)
        return { show }
      },
      template: `
        <div>
          <button data-testid="toggle" @click="show = !show">Toggle</button>
          <KeepAlive>
            <InnerComponent v-if="show" key="test" />
          </KeepAlive>
        </div>
      `,
    })

    const mountKeepAliveHarness = async () => {
      const wrapper = mount(WrapperComponent)
      await flushStart()
      return wrapper
    }

    const innerStopAndExpect = async (
      wrapper: ReturnType<typeof mount>,
      expected: string
    ) => {
      await wrapper.get('[data-testid="inner-stop"]').trigger("click")
      expect(wrapper.get('[data-testid="inner-result"]').text()).toBe(expected)
    }

    it("pauses when deactivated", async () => {
      const wrapper = await mountKeepAliveHarness()

      setTime(1000)

      await wrapper.get('[data-testid="toggle"]').trigger("click")
      setTime(2000)

      await wrapper.get('[data-testid="toggle"]').trigger("click")

      await innerStopAndExpect(wrapper, "1000")
    })

    it("resumes when reactivated", async () => {
      const wrapper = await mountKeepAliveHarness()

      setTime(1000)

      await wrapper.get('[data-testid="toggle"]').trigger("click")
      setTime(2000)

      await wrapper.get('[data-testid="toggle"]').trigger("click")

      setTime(3000)

      await innerStopAndExpect(wrapper, "2000")
    })
  })
})

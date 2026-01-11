import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"
import { mount } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { defineComponent, KeepAlive, onActivated, onDeactivated } from "vue"

describe("useThinkingTimeTracker", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>
  let rafCallbacks: Array<FrameRequestCallback> = []

  beforeEach(() => {
    // Browser Mode: Can use fake timers, but performance.now() is real!
    vi.useFakeTimers()
    // Spy on real performance.now() instead of mocking it
    performanceNowSpy = vi.spyOn(performance, "now")
    performanceNowSpy.mockReturnValue(0)

    rafCallbacks = []
    // Mock requestAnimationFrame for deterministic testing
    // Browser Mode: Use globalThis instead of global
    globalThis.requestAnimationFrame = vi.fn(
      (callback: FrameRequestCallback) => {
        rafCallbacks.push(callback)
        return 1
      }
    ) as unknown as typeof requestAnimationFrame
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  const flushRAF = () => {
    const callbacks = [...rafCallbacks]
    rafCallbacks = []
    callbacks.forEach((cb) => cb(performance.now()))
  }

  const createTestComponent = (setupFn: () => { stop: () => number }) => {
    return defineComponent({
      setup: setupFn,
      template: "<div>Test</div>",
    })
  }

  const setupTimer = async (component: ReturnType<typeof mount>) => {
    await component.vm.$nextTick()
    flushRAF()
  }

  const setTime = (ms: number) => {
    performanceNowSpy.mockReturnValue(ms)
    vi.advanceTimersByTime(ms)
  }

  it("starts timer after nextTick and requestAnimationFrame", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    const wrapper = mount(TestComponent)
    await setupTimer(wrapper)
    setTime(1000)

    expect(wrapper.vm.stop()).toBeGreaterThanOrEqual(1000)
  })

  it("pauses when page becomes hidden", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    const wrapper = mount(TestComponent)
    await setupTimer(wrapper)
    setTime(1000)

    // Browser Mode: Real Page Visibility API!
    // Set document.hidden and dispatch real visibilitychange event
    Object.defineProperty(document, "hidden", {
      value: true,
      writable: true,
      configurable: true,
    })
    document.dispatchEvent(new Event("visibilitychange"))
    setTime(2000)

    expect(wrapper.vm.stop()).toBe(1000)
  })

  it("resumes when page becomes visible", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    const wrapper = mount(TestComponent)
    await setupTimer(wrapper)
    setTime(1000)

    // Browser Mode: Real visibilitychange events work!
    Object.defineProperty(document, "hidden", {
      value: true,
      writable: true,
      configurable: true,
    })
    document.dispatchEvent(new Event("visibilitychange"))
    setTime(2000)

    Object.defineProperty(document, "hidden", {
      value: false,
      writable: true,
      configurable: true,
    })
    document.dispatchEvent(new Event("visibilitychange"))
    setTime(3000)

    expect(wrapper.vm.stop()).toBe(2000)
  })

  it("pauses when window loses focus", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    const wrapper = mount(TestComponent)
    await setupTimer(wrapper)
    setTime(1000)

    // Browser Mode: Real window blur event!
    window.dispatchEvent(new Event("blur"))
    setTime(2000)

    expect(wrapper.vm.stop()).toBe(1000)
  })

  it("resumes when window gains focus", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    const wrapper = mount(TestComponent)
    await setupTimer(wrapper)
    setTime(1000)

    // Browser Mode: Real window focus/blur events!
    window.dispatchEvent(new Event("blur"))
    setTime(2000)

    window.dispatchEvent(new Event("focus"))
    setTime(3000)

    expect(wrapper.vm.stop()).toBe(2000)
  })

  it("only records once per stop call", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    const wrapper = mount(TestComponent)
    await setupTimer(wrapper)
    setTime(1000)

    expect(wrapper.vm.stop()).toBe(wrapper.vm.stop())
  })

  it("returns rounded milliseconds", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    const wrapper = mount(TestComponent)
    await setupTimer(wrapper)
    setTime(1234.567)

    expect(wrapper.vm.stop()).toBe(1235)
  })

  describe("KeepAlive lifecycle", () => {
    let TestComponent: ReturnType<typeof createTestComponent>

    beforeEach(() => {
      TestComponent = createTestComponent(() => {
        const { start, stop, pause, resume } = useThinkingTimeTracker()
        onActivated(() => {
          start()
          resume()
        })
        onDeactivated(() => pause())
        start()
        return { stop }
      })
    })

    const createKeepAliveWrapper = () => {
      return defineComponent({
        components: { TestComponent, KeepAlive },
        data() {
          return { show: true }
        },
        template: `<KeepAlive><TestComponent v-if="show" key="test" /></KeepAlive>`,
      })
    }

    it("pauses when deactivated", async () => {
      const wrapper = mount(createKeepAliveWrapper())
      const testComponent = wrapper.findAllComponents(TestComponent)[0]
      expect(testComponent).toBeDefined()
      if (!testComponent) return
      await setupTimer(testComponent)
      setTime(1000)

      await wrapper.setData({ show: false })
      await wrapper.vm.$nextTick()
      setTime(2000)

      expect(testComponent.vm.stop()).toBe(1000)
    })

    it("resumes when reactivated", async () => {
      const wrapper = mount(createKeepAliveWrapper())
      const testComponent = wrapper.findAllComponents(TestComponent)[0]
      expect(testComponent).toBeDefined()
      if (!testComponent) return
      await setupTimer(testComponent)
      setTime(1000)

      await wrapper.setData({ show: false })
      await wrapper.vm.$nextTick()
      setTime(2000)

      await wrapper.setData({ show: true })
      await setupTimer(testComponent)
      setTime(3000)

      expect(testComponent.vm.stop()).toBe(2000)
    })
  })
})

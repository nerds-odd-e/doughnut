import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { mount } from "@vue/test-utils"
import { defineComponent, KeepAlive, onActivated, onDeactivated } from "vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"

describe("useThinkingTimeTracker", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>
  let rafCallbacks: Array<FrameRequestCallback> = []

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
    rafCallbacks = []
    global.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
      rafCallbacks.push(callback)
      return 1
    }) as unknown as typeof requestAnimationFrame
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

  it("starts timer after nextTick and requestAnimationFrame", async () => {
    const TestComponent = defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        start()
        return { stop }
      },
      template: "<div>Test</div>",
    })

    const wrapper = mount(TestComponent)
    await wrapper.vm.$nextTick()
    flushRAF()

    vi.advanceTimersByTime(1000)
    performanceNowSpy.mockReturnValue(1000)

    const time = wrapper.vm.stop()
    expect(time).toBeGreaterThanOrEqual(1000)
  })

  it("pauses when page becomes hidden", async () => {
    const TestComponent = defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        start()
        return { stop }
      },
      template: "<div>Test</div>",
    })

    const wrapper = mount(TestComponent)
    await wrapper.vm.$nextTick()
    flushRAF()

    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    Object.defineProperty(document, "hidden", { value: true, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    performanceNowSpy.mockReturnValue(2000)
    vi.advanceTimersByTime(1000)

    const time = wrapper.vm.stop()
    expect(time).toBe(1000)
  })

  it("resumes when page becomes visible", async () => {
    const TestComponent = defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        start()
        return { stop }
      },
      template: "<div>Test</div>",
    })

    const wrapper = mount(TestComponent)
    await wrapper.vm.$nextTick()
    flushRAF()

    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    Object.defineProperty(document, "hidden", { value: true, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    performanceNowSpy.mockReturnValue(2000)
    vi.advanceTimersByTime(1000)

    Object.defineProperty(document, "hidden", { value: false, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    performanceNowSpy.mockReturnValue(3000)
    vi.advanceTimersByTime(1000)

    const time = wrapper.vm.stop()
    expect(time).toBe(2000)
  })

  it("pauses when window loses focus", async () => {
    const TestComponent = defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        start()
        return { stop }
      },
      template: "<div>Test</div>",
    })

    const wrapper = mount(TestComponent)
    await wrapper.vm.$nextTick()
    flushRAF()

    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    window.dispatchEvent(new Event("blur"))

    performanceNowSpy.mockReturnValue(2000)
    vi.advanceTimersByTime(1000)

    const time = wrapper.vm.stop()
    expect(time).toBe(1000)
  })

  it("resumes when window gains focus", async () => {
    const TestComponent = defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        start()
        return { stop }
      },
      template: "<div>Test</div>",
    })

    const wrapper = mount(TestComponent)
    await wrapper.vm.$nextTick()
    flushRAF()

    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    window.dispatchEvent(new Event("blur"))

    performanceNowSpy.mockReturnValue(2000)
    vi.advanceTimersByTime(1000)

    window.dispatchEvent(new Event("focus"))

    performanceNowSpy.mockReturnValue(3000)
    vi.advanceTimersByTime(1000)

    const time = wrapper.vm.stop()
    expect(time).toBe(2000)
  })

  it("only records once per stop call", async () => {
    const TestComponent = defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        start()
        return { stop }
      },
      template: "<div>Test</div>",
    })

    const wrapper = mount(TestComponent)
    await wrapper.vm.$nextTick()
    flushRAF()

    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    const time1 = wrapper.vm.stop()
    const time2 = wrapper.vm.stop()

    expect(time1).toBe(time2)
  })

  it("returns rounded milliseconds", async () => {
    const TestComponent = defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        start()
        return { stop }
      },
      template: "<div>Test</div>",
    })

    const wrapper = mount(TestComponent)
    await wrapper.vm.$nextTick()
    flushRAF()

    performanceNowSpy.mockReturnValue(1234.567)
    vi.advanceTimersByTime(1234.567)

    const time = wrapper.vm.stop()
    expect(time).toBe(1235)
  })

  it("pauses when component is deactivated (KeepAlive)", async () => {
    const TestComponent = defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        onActivated(() => {
          start()
        })
        onDeactivated(() => {
          // pause/resume removed as part of revert
        })
        start()
        return { stop }
      },
      template: "<div>Test</div>",
    })

    const WrapperComponent = defineComponent({
      components: { TestComponent, KeepAlive },
      data() {
        return { show: true }
      },
      template: `
        <KeepAlive>
          <TestComponent v-if="show" key="test" />
        </KeepAlive>
      `,
    })

    const wrapper = mount(WrapperComponent)
    const testComponent = wrapper.findComponent(TestComponent)
    await testComponent.vm.$nextTick()
    flushRAF()

    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    // Simulate component deactivation (switching away)
    await wrapper.setData({ show: false })
    await wrapper.vm.$nextTick()

    performanceNowSpy.mockReturnValue(2000)
    vi.advanceTimersByTime(1000)

    // Component is still alive but deactivated, so timer should be paused
    const time = testComponent.vm.stop()
    expect(time).toBe(1000)
  })

  it("resumes when component is reactivated (KeepAlive)", async () => {
    const TestComponent = defineComponent({
      setup() {
        const { start, stop } = useThinkingTimeTracker()
        onActivated(() => {
          start()
        })
        onDeactivated(() => {
          // pause/resume removed as part of revert
        })
        start()
        return { stop }
      },
      template: "<div>Test</div>",
    })

    const WrapperComponent = defineComponent({
      components: { TestComponent, KeepAlive },
      data() {
        return { show: true }
      },
      template: `
        <KeepAlive>
          <TestComponent v-if="show" key="test" />
        </KeepAlive>
      `,
    })

    const wrapper = mount(WrapperComponent)
    const testComponent = wrapper.findComponent(TestComponent)
    await testComponent.vm.$nextTick()
    flushRAF()

    performanceNowSpy.mockReturnValue(1000)
    vi.advanceTimersByTime(1000)

    // Simulate component deactivation
    await wrapper.setData({ show: false })
    await wrapper.vm.$nextTick()

    performanceNowSpy.mockReturnValue(2000)
    vi.advanceTimersByTime(1000)

    // Simulate component reactivation
    await wrapper.setData({ show: true })
    await testComponent.vm.$nextTick()
    flushRAF()

    performanceNowSpy.mockReturnValue(3000)
    vi.advanceTimersByTime(1000)

    const time = testComponent.vm.stop()
    expect(time).toBe(2000)
  })
})

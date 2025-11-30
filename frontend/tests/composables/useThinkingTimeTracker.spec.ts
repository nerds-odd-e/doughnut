import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { mount } from "@vue/test-utils"
import { defineComponent } from "vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"

describe("useThinkingTimeTracker", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.useFakeTimers()
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

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
    await new Promise((resolve) => requestAnimationFrame(resolve))

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
    await new Promise((resolve) => requestAnimationFrame(resolve))

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
    await new Promise((resolve) => requestAnimationFrame(resolve))

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
    await new Promise((resolve) => requestAnimationFrame(resolve))

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
    await new Promise((resolve) => requestAnimationFrame(resolve))

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
    await new Promise((resolve) => requestAnimationFrame(resolve))

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
    await new Promise((resolve) => requestAnimationFrame(resolve))

    performanceNowSpy.mockReturnValue(1234.567)
    vi.advanceTimersByTime(1234.567)

    const time = wrapper.vm.stop()
    expect(time).toBe(1235)
  })
})

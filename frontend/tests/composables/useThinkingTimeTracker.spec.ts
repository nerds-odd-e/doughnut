import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { render } from "vitest-browser-vue"
import {
  defineComponent,
  ref,
  KeepAlive,
  onActivated,
  onDeactivated,
} from "vue"
import { useThinkingTimeTracker } from "@/composables/useThinkingTimeTracker"
import { page } from "vitest/browser"

describe("useThinkingTimeTracker", () => {
  let performanceNowSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.useFakeTimers()
    // performance.now() is available in browser environment
    performanceNowSpy = vi.spyOn(performance, "now").mockReturnValue(0)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  const createTestComponent = (
    setupFn: () => {
      stop: () => number
      pause?: () => void
      resume?: () => void
    }
  ) =>
    defineComponent({
      setup() {
        const { stop, pause, resume } = setupFn()
        const result = ref<number | null>(null)

        const handleStop = () => {
          result.value = stop()
        }

        return { handleStop, result, pause, resume }
      },
      template: `
        <div>
          <button data-testid="stop" @click="handleStop">Stop</button>
          <button data-testid="pause" @click="pause && pause()">Pause</button>
          <button data-testid="resume" @click="resume && resume()">Resume</button>
          <span data-testid="result">{{ result }}</span>
        </div>
      `,
    })

  const setTime = async (ms: number) => {
    performanceNowSpy.mockReturnValue(ms)
    await vi.advanceTimersByTimeAsync(ms)
  }

  it("starts timer after nextTick and requestAnimationFrame", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    render(TestComponent)

    // Initial wait to let onMounted/nextTick happen
    await vi.advanceTimersToNextTimerAsync()

    await setTime(1000)

    await page.getByTestId("stop").click()
    const result = page.getByTestId("result")

    // We expect >= 1000.
    // Since we control performance.now returning exactly 1000, it should be 1000.
    // However, the original test used expected >= 1000.
    await expect.element(result).toHaveTextContent("1000")
  })

  it("pauses when page becomes hidden", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    render(TestComponent)
    await vi.advanceTimersToNextTimerAsync()

    await setTime(1000)

    // Simulate visibility change
    Object.defineProperty(document, "hidden", { value: true, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    await setTime(2000) // Advance another 1000ms (total 2000)

    await page.getByTestId("stop").click()
    // Should have only counted the first 1000ms
    await expect.element(page.getByTestId("result")).toHaveTextContent("1000")
  })

  it("resumes when page becomes visible", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    render(TestComponent)
    await vi.advanceTimersToNextTimerAsync()

    await setTime(1000)

    Object.defineProperty(document, "hidden", { value: true, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    await setTime(2000) // Total 2000, paused at 1000

    Object.defineProperty(document, "hidden", { value: false, writable: true })
    document.dispatchEvent(new Event("visibilitychange"))

    await setTime(3000) // Total 3000, resumed at 2000. +1000ms more. Total tracked: 2000.

    await page.getByTestId("stop").click()
    await expect.element(page.getByTestId("result")).toHaveTextContent("2000")
  })

  it("pauses when window loses focus", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    render(TestComponent)
    await vi.advanceTimersToNextTimerAsync()

    await setTime(1000)

    window.dispatchEvent(new Event("blur"))
    await setTime(2000)

    await page.getByTestId("stop").click()
    await expect.element(page.getByTestId("result")).toHaveTextContent("1000")
  })

  it("resumes when window gains focus", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    render(TestComponent)
    await vi.advanceTimersToNextTimerAsync()

    await setTime(1000)

    window.dispatchEvent(new Event("blur"))
    await setTime(2000)

    window.dispatchEvent(new Event("focus"))
    await setTime(3000)

    await page.getByTestId("stop").click()
    await expect.element(page.getByTestId("result")).toHaveTextContent("2000")
  })

  it("only records once per stop call", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    render(TestComponent)
    await vi.advanceTimersToNextTimerAsync()

    await setTime(1000)

    await page.getByTestId("stop").click()
    await expect.element(page.getByTestId("result")).toHaveTextContent("1000")

    // In the original test: expect(wrapper.vm.stop()).toBe(wrapper.vm.stop())
    // Which means subsequent calls return the same value?
    // Let's check logic: if I click stop again, it should update result to the same value?

    await page.getByTestId("stop").click()
    await expect.element(page.getByTestId("result")).toHaveTextContent("1000")
  })

  it("returns rounded milliseconds", async () => {
    const TestComponent = createTestComponent(() => {
      const { start, stop } = useThinkingTimeTracker()
      start()
      return { stop }
    })

    render(TestComponent)
    await vi.advanceTimersToNextTimerAsync()

    await setTime(1234.567)

    await page.getByTestId("stop").click()
    await expect.element(page.getByTestId("result")).toHaveTextContent("1235")
  })

  describe("KeepAlive lifecycle", () => {
    // We need a wrapper component to test KeepAlive
    const InnerComponent = defineComponent({
      setup() {
        const { start, stop, pause, resume } = useThinkingTimeTracker()
        const result = ref<number | null>(null)

        onActivated(() => {
          start()
          resume()
        })
        onDeactivated(() => pause())

        // Original test called start() immediately in setup
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

    it("pauses when deactivated", async () => {
      render(WrapperComponent)
      await vi.advanceTimersToNextTimerAsync()

      await setTime(1000)

      // Deactivate
      await page.getByTestId("toggle").click()
      await setTime(2000)

      // We need to access the component state. But it is hidden/deactivated.
      // However, KeepAlive keeps the instance.
      // We can toggle it back to see the result, or we can check if it paused.
      // If we stop() while deactivated, what happens?
      // The original test accessed `testComponent.vm.stop()`.

      // If the component is v-if=false, it is removed from DOM (put in cache).
      // We cannot interact with its buttons via DOM queries easily if it's not in DOM.

      // Let's assume we toggle back.
      // At T=1000, deactivated.
      // At T=2000, reactivated. onActivated -> resume.
      // If we check immediately at T=2000, it should be 1000?

      await page.getByTestId("toggle").click() // Show again
      // It resumes.

      await page.getByTestId("inner-stop").click()
      // Should be 1000 because we just resumed and didn't advance time further than 2000.
      await expect
        .element(page.getByTestId("inner-result"))
        .toHaveTextContent("1000")
    })

    it("resumes when reactivated", async () => {
      render(WrapperComponent)
      await vi.advanceTimersToNextTimerAsync()

      await setTime(1000)

      // Deactivate
      await page.getByTestId("toggle").click()
      await setTime(2000)

      // Reactivate
      await page.getByTestId("toggle").click()
      // onActivated -> start() + resume().
      // note: useThinkingTimeTracker start() resets if called again?
      // Original:
      // onActivated(() => { start(); resume(); })
      // useThinkingTimeTracker logic: start() sets startTime if not set? Or resets?
      // We assume it resumes tracking.

      await setTime(3000) // +1000ms active

      await page.getByTestId("inner-stop").click()
      await expect
        .element(page.getByTestId("inner-result"))
        .toHaveTextContent("2000")
    })
  })
})

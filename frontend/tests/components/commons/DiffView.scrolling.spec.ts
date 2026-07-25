import DiffView from "@/components/commons/DiffView.vue"
import helper from "@tests/helpers"
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest"
import { screen, fireEvent } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"

describe("DiffView", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe("synchronized scrolling", () => {
    const generateLongContent = (lineCount: number) =>
      Array.from({ length: lineCount }, (_, i) => `Line ${i + 1} content`).join(
        "\n"
      )

    it("synchronizes right pane scroll when left pane is scrolled", async () => {
      const longContent = generateLongContent(50)

      helper
        .component(DiffView)
        .withProps({
          current: longContent,
          old: longContent,
          maxHeight: "100px",
        })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      Object.defineProperty(leftPane, "scrollTop", {
        writable: true,
        value: 100,
      })
      Object.defineProperty(rightPane, "scrollTop", {
        writable: true,
        value: 0,
      })

      await fireEvent.scroll(leftPane)
      vi.advanceTimersByTime(100)
      await flushPromises()

      expect(rightPane.scrollTop).toBe(100)
    })

    it("synchronizes left pane scroll when right pane is scrolled", async () => {
      const longContent = generateLongContent(50)

      helper
        .component(DiffView)
        .withProps({
          current: longContent,
          old: longContent,
          maxHeight: "100px",
        })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      Object.defineProperty(rightPane, "scrollTop", {
        writable: true,
        value: 150,
      })
      Object.defineProperty(leftPane, "scrollTop", {
        writable: true,
        value: 0,
      })

      await fireEvent.scroll(rightPane)
      vi.advanceTimersByTime(100)
      await flushPromises()

      expect(leftPane.scrollTop).toBe(150)
    })

    it("scroll synchronization settles to a stable value without oscillation", async () => {
      const longContent = generateLongContent(100)

      helper
        .component(DiffView)
        .withProps({
          current: longContent,
          old: longContent,
          maxHeight: "100px",
        })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      let _leftScrollCalls = 0
      let rightScrollCalls = 0

      const originalLeftScrollTop = Object.getOwnPropertyDescriptor(
        leftPane,
        "scrollTop"
      )
      const originalRightScrollTop = Object.getOwnPropertyDescriptor(
        rightPane,
        "scrollTop"
      )

      let leftValue = 0
      let rightValue = 0

      Object.defineProperty(leftPane, "scrollTop", {
        get: () => leftValue,
        set: (v: number) => {
          leftValue = v
          _leftScrollCalls++
        },
        configurable: true,
      })

      Object.defineProperty(rightPane, "scrollTop", {
        get: () => rightValue,
        set: (v: number) => {
          rightValue = v
          rightScrollCalls++
        },
        configurable: true,
      })

      leftValue = 200
      await fireEvent.scroll(leftPane)
      vi.advanceTimersByTime(100)
      await flushPromises()

      vi.advanceTimersByTime(200)
      await flushPromises()

      expect(rightScrollCalls).toBeLessThanOrEqual(2)

      if (originalLeftScrollTop) {
        Object.defineProperty(leftPane, "scrollTop", originalLeftScrollTop)
      }
      if (originalRightScrollTop) {
        Object.defineProperty(rightPane, "scrollTop", originalRightScrollTop)
      }
    })

    it("synchronizes horizontal scroll as well", async () => {
      const wideContent = `${"A".repeat(500)}\n${"B".repeat(500)}`

      helper
        .component(DiffView)
        .withProps({
          current: wideContent,
          old: wideContent,
          maxHeight: "100px",
        })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      Object.defineProperty(leftPane, "scrollLeft", {
        writable: true,
        value: 50,
      })
      Object.defineProperty(rightPane, "scrollLeft", {
        writable: true,
        value: 0,
      })

      await fireEvent.scroll(leftPane)
      vi.advanceTimersByTime(100)
      await flushPromises()

      expect(rightPane.scrollLeft).toBe(50)
    })
  })
})

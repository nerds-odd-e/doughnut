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

  describe("line alignment and placeholders", () => {
    it("renders the same number of visible diff rows for both panes when lines are inserted", () => {
      const oldText = "line1\nline2\nline3"
      const currentText = "line1\ninserted line\nline2\nline3"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      const leftRows = leftPane.querySelectorAll(".diff-row")
      const rightRows = rightPane.querySelectorAll(".diff-row")

      expect(leftRows.length).toBe(rightRows.length)
    })

    it("renders the same number of visible diff rows for both panes when lines are deleted", () => {
      const oldText = "line1\ndeleted line\nline2\nline3"
      const currentText = "line1\nline2\nline3"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      const leftRows = leftPane.querySelectorAll(".diff-row")
      const rightRows = rightPane.querySelectorAll(".diff-row")

      expect(leftRows.length).toBe(rightRows.length)
    })

    it("shows placeholder rows on the right side when lines are added to current", () => {
      const oldText = "line1\nline2"
      const currentText = "line1\nnew line\nline2"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const rightPane = screen.getByTestId("diff-right-pane")
      const placeholderCells = rightPane.querySelectorAll(
        "[data-placeholder='true']"
      )

      expect(placeholderCells.length).toBeGreaterThan(0)
    })

    it("shows placeholder rows on the left side when lines exist only in old", () => {
      const oldText = "line1\nold line\nline2"
      const currentText = "line1\nline2"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const placeholderCells = leftPane.querySelectorAll(
        "[data-placeholder='true']"
      )

      expect(placeholderCells.length).toBeGreaterThan(0)
    })

    it("aligns corresponding text in the same row index when insertions occur in the middle", () => {
      const oldText = "first\nsecond\nthird"
      const currentText = "first\ninserted\nsecond\nthird"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      const leftRows = leftPane.querySelectorAll(".diff-row")
      const rightRows = rightPane.querySelectorAll(".diff-row")

      const lastLeftRowText =
        leftRows[leftRows.length - 1]?.querySelector(
          ".diff-content-cell"
        )?.textContent
      const lastRightRowText =
        rightRows[rightRows.length - 1]?.querySelector(
          ".diff-content-cell"
        )?.textContent

      expect(lastLeftRowText).toBe("third")
      expect(lastRightRowText).toBe("third")
    })

    it("handles multiple consecutive insertions with proper placeholder alignment", () => {
      const oldText = "start\nend"
      const currentText = "start\ninsert1\ninsert2\ninsert3\nend"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const rightPane = screen.getByTestId("diff-right-pane")
      const placeholderCells = rightPane.querySelectorAll(
        "[data-placeholder='true']"
      )

      expect(placeholderCells.length).toBe(3)
    })

    it("handles multiple consecutive deletions with proper placeholder alignment", () => {
      const oldText = "start\ndelete1\ndelete2\ndelete3\nend"
      const currentText = "start\nend"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const placeholderCells = leftPane.querySelectorAll(
        "[data-placeholder='true']"
      )

      expect(placeholderCells.length).toBe(3)
    })
  })

  describe("line numbers", () => {
    it("displays line number columns on both sides", () => {
      const text = "line1\nline2\nline3"

      helper
        .component(DiffView)
        .withProps({ current: text, old: text })
        .render()

      const leftLineNumbers = screen.getAllByTestId("line-number-left")
      const rightLineNumbers = screen.getAllByTestId("line-number-right")

      expect(leftLineNumbers.length).toBeGreaterThan(0)
      expect(rightLineNumbers.length).toBeGreaterThan(0)
    })

    it("displays correct line numbers for normal rows", () => {
      const text = "line1\nline2\nline3"

      helper
        .component(DiffView)
        .withProps({ current: text, old: text })
        .render()

      const leftLineNumbers = screen.getAllByTestId("line-number-left")
      const rightLineNumbers = screen.getAllByTestId("line-number-right")

      expect(leftLineNumbers[0]?.textContent?.trim()).toBe("1")
      expect(leftLineNumbers[1]?.textContent?.trim()).toBe("2")
      expect(leftLineNumbers[2]?.textContent?.trim()).toBe("3")

      expect(rightLineNumbers[0]?.textContent?.trim()).toBe("1")
      expect(rightLineNumbers[1]?.textContent?.trim()).toBe("2")
      expect(rightLineNumbers[2]?.textContent?.trim()).toBe("3")
    })

    it("displays empty line numbers for placeholder rows", () => {
      const oldText = "line1\nline2"
      const currentText = "line1\ninserted\nline2"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const rightPane = screen.getByTestId("diff-right-pane")
      const placeholderLineNumbers = rightPane.querySelectorAll(
        ".diff-line-number.diff-placeholder"
      )

      for (const lineNumber of placeholderLineNumbers) {
        expect(lineNumber.textContent?.trim()).toBe("")
      }
    })

    it("maintains correct line numbers on left side when insertions exist on current", () => {
      const oldText = "first\nsecond"
      const currentText = "first\ninserted\nsecond"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const leftLineNumbers = leftPane.querySelectorAll(
        "[data-testid='line-number-left']"
      )

      expect(leftLineNumbers[0]?.textContent?.trim()).toBe("1")
      expect(leftLineNumbers[1]?.textContent?.trim()).toBe("2")
      expect(leftLineNumbers[2]?.textContent?.trim()).toBe("3")
    })

    it("maintains correct line numbers on right side when deletions exist from old", () => {
      const oldText = "first\ndeleted\nsecond"
      const currentText = "first\nsecond"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const rightPane = screen.getByTestId("diff-right-pane")
      const rightLineNumbers = rightPane.querySelectorAll(
        "[data-testid='line-number-right']"
      )

      expect(rightLineNumbers[0]?.textContent?.trim()).toBe("1")
      expect(rightLineNumbers[1]?.textContent?.trim()).toBe("2")
      expect(rightLineNumbers[2]?.textContent?.trim()).toBe("3")
    })
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

  describe("diff highlighting", () => {
    it("highlights added lines in the current pane", () => {
      const oldText = "line1\nline2"
      const currentText = "line1\nnew line\nline2"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const addedCells = leftPane.querySelectorAll(".diff-added")

      expect(addedCells.length).toBeGreaterThan(0)
      expect(addedCells[0]?.textContent).toBe("new line")
    })

    it("highlights removed lines in the old pane", () => {
      const oldText = "line1\nremoved line\nline2"
      const currentText = "line1\nline2"

      helper
        .component(DiffView)
        .withProps({ current: currentText, old: oldText })
        .render()

      const rightPane = screen.getByTestId("diff-right-pane")
      const removedCells = rightPane.querySelectorAll(".diff-removed")

      expect(removedCells.length).toBeGreaterThan(0)
      expect(removedCells[0]?.textContent).toBe("removed line")
    })

    it("does not highlight identical content", () => {
      const text = "line1\nline2\nline3"

      helper
        .component(DiffView)
        .withProps({ current: text, old: text })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      const addedCells = leftPane.querySelectorAll(".diff-added")
      const removedCells = rightPane.querySelectorAll(".diff-removed")

      expect(addedCells.length).toBe(0)
      expect(removedCells.length).toBe(0)
    })
  })

  describe("rendering behavior", () => {
    it("uses monospace font for content", () => {
      const text = "line1\nline2"

      helper
        .component(DiffView)
        .withProps({ current: text, old: text })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const table = leftPane.querySelector(".diff-table")

      expect(table?.classList.contains("daisy-font-mono")).toBe(true)
    })

    it("displays Current and Will restore to labels", () => {
      const text = "content"

      helper
        .component(DiffView)
        .withProps({ current: text, old: text })
        .render()

      expect(screen.getByText("Current")).toBeInTheDocument()
      expect(screen.getByText("Will restore to")).toBeInTheDocument()
    })

    it("respects maxHeight prop for scrollable areas", () => {
      const text = "line1\nline2\nline3"

      helper
        .component(DiffView)
        .withProps({ current: text, old: text, maxHeight: "150px" })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      expect(leftPane.style.maxHeight).toBe("150px")
      expect(rightPane.style.maxHeight).toBe("150px")
    })

    it("handles empty strings gracefully", () => {
      helper.component(DiffView).withProps({ current: "", old: "" }).render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const rightPane = screen.getByTestId("diff-right-pane")

      expect(leftPane).toBeInTheDocument()
      expect(rightPane).toBeInTheDocument()
    })

    it("handles single line content", () => {
      helper
        .component(DiffView)
        .withProps({ current: "single line", old: "single line" })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const leftRows = leftPane.querySelectorAll(".diff-row")

      expect(leftRows.length).toBe(1)
    })
  })
})

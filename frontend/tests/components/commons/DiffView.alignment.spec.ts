import DiffView from "@/components/commons/DiffView.vue"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"
import { screen } from "@testing-library/vue"

describe("DiffView", () => {
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
})

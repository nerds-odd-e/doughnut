import DiffView from "@/components/commons/DiffView.vue"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"
import { screen } from "@testing-library/vue"

describe("DiffView", () => {
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
})

import DiffView from "@/components/commons/DiffView.vue"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"
import { screen } from "@testing-library/vue"

describe("DiffView", () => {
  describe("rendering behavior", () => {
    it("uses monospace font for content", () => {
      const text = "line1\nline2"

      helper
        .component(DiffView)
        .withProps({ current: text, old: text })
        .render()

      const leftPane = screen.getByTestId("diff-left-pane")
      const table = leftPane.querySelector(".diff-table")

      expect(table?.classList.contains("font-mono")).toBe(true)
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

    it("displays custom pane labels when provided", () => {
      const text = "content"

      helper
        .component(DiffView)
        .withProps({
          current: text,
          old: text,
          currentLabel: "Original",
          oldLabel: "Updated",
        })
        .render()

      expect(screen.getByText("Original")).toBeInTheDocument()
      expect(screen.getByText("Updated")).toBeInTheDocument()
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

      expect(screen.getByTestId("diff-left-pane")).toBeInTheDocument()
      expect(screen.getByTestId("diff-right-pane")).toBeInTheDocument()
    })
  })
})

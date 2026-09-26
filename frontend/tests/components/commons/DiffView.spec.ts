import DiffView from "@/components/commons/DiffView.vue"
import helper from "@tests/helpers"
import { afterEach, beforeEach, describe, it, expect, vi } from "vitest"
import { screen, fireEvent } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"

type Side = "left" | "right"

function renderDiff(props: {
  old: string
  current: string
  maxHeight?: string
  currentLabel?: string
  oldLabel?: string
}) {
  helper.component(DiffView).withProps(props).render()
  const panes = {
    left: screen.getByTestId("diff-left-pane"),
    right: screen.getByTestId("diff-right-pane"),
  }
  const lineNumbers = (side: Side) =>
    Array.from(
      panes[side].querySelectorAll(`[data-testid='line-number-${side}']`),
      (cell) => cell.textContent?.trim()
    )
  return { ...panes, lineNumbers }
}

describe("DiffView", () => {
  it("renders monospace panes with default labels and the given maxHeight", () => {
    const { left, right } = renderDiff({
      current: "line1\nline2",
      old: "line1\nline2",
      maxHeight: "150px",
    })

    expect(left.querySelector(".diff-table")?.classList).toContain("font-mono")
    expect(screen.getByText("Current")).toBeInTheDocument()
    expect(screen.getByText("Will restore to")).toBeInTheDocument()
    expect(left.style.maxHeight).toBe("150px")
    expect(right.style.maxHeight).toBe("150px")
  })

  it("displays custom pane labels when provided", () => {
    renderDiff({
      current: "content",
      old: "content",
      currentLabel: "Original",
      oldLabel: "Updated",
    })

    expect(screen.getByText("Original")).toBeInTheDocument()
    expect(screen.getByText("Updated")).toBeInTheDocument()
  })

  it("handles empty strings gracefully", () => {
    const { left, right } = renderDiff({ current: "", old: "" })

    expect(left).toBeInTheDocument()
    expect(right).toBeInTheDocument()
  })

  it.each([
    {
      side: "left",
      cls: ".diff-added",
      old: "a\nb",
      current: "a\nnew line\nb",
      text: "new line",
    },
    {
      side: "right",
      cls: ".diff-removed",
      old: "a\nremoved line\nb",
      current: "a\nb",
      text: "removed line",
    },
  ] as const)(
    "highlights $text in the $side pane",
    ({ side, cls, old, current, text }) => {
      const panes = renderDiff({ old, current })

      expect(
        Array.from(panes[side].querySelectorAll(cls), (c) => c.textContent)
      ).toEqual([text])
    }
  )

  it("neither highlights nor misnumbers identical content", () => {
    const { left, right, lineNumbers } = renderDiff({
      current: "line1\nline2\nline3",
      old: "line1\nline2\nline3",
    })

    expect(left.querySelectorAll(".diff-added")).toHaveLength(0)
    expect(right.querySelectorAll(".diff-removed")).toHaveLength(0)
    expect(lineNumbers("left")).toEqual(["1", "2", "3"])
    expect(lineNumbers("right")).toEqual(["1", "2", "3"])
  })

  it.each([
    {
      change: "one insertion",
      old: "first\nsecond",
      current: "first\ninserted\nsecond",
      placeholders: "right",
      count: 1,
    },
    {
      change: "one deletion",
      old: "first\ndeleted\nsecond",
      current: "first\nsecond",
      placeholders: "left",
      count: 1,
    },
    {
      change: "consecutive insertions",
      old: "start\nend",
      current: "start\ni1\ni2\ni3\nend",
      placeholders: "right",
      count: 3,
    },
    {
      change: "consecutive deletions",
      old: "start\nd1\nd2\nd3\nend",
      current: "start\nend",
      placeholders: "left",
      count: 3,
    },
  ] as const)(
    "aligns both panes around $change with $count $placeholders placeholder rows",
    ({ old, current, placeholders, count }) => {
      const panes = renderDiff({ old, current })
      const numbered: Side = placeholders === "left" ? "right" : "left"
      const rows = (side: Side) => panes[side].querySelectorAll(".diff-row")
      const lastRowText = (side: Side) =>
        [...rows(side)].at(-1)?.querySelector(".diff-content-cell")?.textContent

      expect(rows("left").length).toBe(rows("right").length)
      expect(lastRowText("left")).toBe(lastRowText("right"))
      expect(
        panes[placeholders].querySelectorAll("[data-placeholder='true']")
      ).toHaveLength(count)
      for (const cell of panes[placeholders].querySelectorAll(
        ".diff-line-number.diff-placeholder"
      )) {
        expect(cell.textContent?.trim()).toBe("")
      }
      expect(panes.lineNumbers(numbered).slice(0, 3)).toEqual(["1", "2", "3"])
    }
  )
})

describe("DiffView synchronized scrolling", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const longContent = (lineCount: number) =>
    Array.from({ length: lineCount }, (_, i) => `Line ${i + 1} content`).join(
      "\n"
    )

  const scrollAndSettle = async (pane: HTMLElement) => {
    await fireEvent.scroll(pane)
    vi.advanceTimersByTime(100)
    await flushPromises()
  }

  it.each([
    {
      from: "left",
      to: "right",
      axis: "scrollTop",
      content: longContent(50),
      value: 100,
    },
    {
      from: "right",
      to: "left",
      axis: "scrollTop",
      content: longContent(50),
      value: 150,
    },
    {
      from: "left",
      to: "right",
      axis: "scrollLeft",
      content: `${"A".repeat(500)}\n${"B".repeat(500)}`,
      value: 50,
    },
  ] as const)(
    "copies $axis from the $from pane to the $to pane",
    async ({ from, to, axis, content, value }) => {
      const panes = renderDiff({
        current: content,
        old: content,
        maxHeight: "100px",
      })
      Object.defineProperty(panes[from], axis, { writable: true, value })
      Object.defineProperty(panes[to], axis, { writable: true, value: 0 })

      await scrollAndSettle(panes[from])

      expect(panes[to][axis]).toBe(value)
    }
  )

  it("scroll synchronization settles to a stable value without oscillation", async () => {
    const content = longContent(100)
    const { left, right } = renderDiff({
      current: content,
      old: content,
      maxHeight: "100px",
    })
    let rightScrollCalls = 0
    let leftValue = 200
    let rightValue = 0
    Object.defineProperty(left, "scrollTop", {
      get: () => leftValue,
      set: (v: number) => {
        leftValue = v
      },
      configurable: true,
    })
    Object.defineProperty(right, "scrollTop", {
      get: () => rightValue,
      set: (v: number) => {
        rightValue = v
        rightScrollCalls++
      },
      configurable: true,
    })

    await scrollAndSettle(left)
    vi.advanceTimersByTime(200)
    await flushPromises()

    expect(rightScrollCalls).toBeLessThanOrEqual(2)
  })
})

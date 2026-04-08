import {
  attachBookRangeSelectionBboxHighlight,
  BOOK_RANGE_SELECTION_BBOX_HIGHLIGHT_FADE_MS,
} from "@/lib/book-reading/bookRangeSelectionBboxHighlight"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("attachBookRangeSelectionBboxHighlight", () => {
  let host: HTMLDivElement

  beforeEach(() => {
    host = document.createElement("div")
    document.body.appendChild(host)
  })

  afterEach(() => {
    host.remove()
    vi.useRealTimers()
  })

  it("places overlay at the given pixel rect, fades to zero, then removes", () => {
    vi.useFakeTimers()
    attachBookRangeSelectionBboxHighlight(host, {
      left: 80,
      top: 120,
      width: 160,
      height: 120,
    })
    const el = host.querySelector(
      "[data-testid=book-range-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.style.left).toBe("80px")
    expect(el.style.top).toBe("120px")
    expect(el.style.width).toBe("160px")
    expect(el.style.height).toBe("120px")
    expect(el.style.transition).toContain(
      `${BOOK_RANGE_SELECTION_BBOX_HIGHLIGHT_FADE_MS}ms`
    )
    expect(el.style.opacity).toBe("1")
    vi.advanceTimersByTime(0)
    expect(el.style.opacity).toBe("0")
    vi.advanceTimersByTime(BOOK_RANGE_SELECTION_BBOX_HIGHLIGHT_FADE_MS)
    expect(
      host.querySelector("[data-testid=book-range-selection-bbox-highlight]")
    ).toBeNull()
  })

  it("cancel() removes the highlight and pending timers", () => {
    vi.useFakeTimers()
    const cancel = attachBookRangeSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 100,
      height: 100,
    })
    cancel()
    expect(
      host.querySelector("[data-testid=book-range-selection-bbox-highlight]")
    ).toBeNull()
    vi.advanceTimersByTime(BOOK_RANGE_SELECTION_BBOX_HIGHLIGHT_FADE_MS)
    expect(
      host.querySelector("[data-testid=book-range-selection-bbox-highlight]")
    ).toBeNull()
  })
})

import {
  attachOutlineSelectionBboxHighlight,
  OUTLINE_SELECTION_BBOX_HIGHLIGHT_FADE_MS,
} from "@/lib/book-reading/outlineSelectionBboxHighlight"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("attachOutlineSelectionBboxHighlight", () => {
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
    attachOutlineSelectionBboxHighlight(host, {
      left: 80,
      top: 120,
      width: 160,
      height: 120,
    })
    const el = host.querySelector(
      "[data-testid=outline-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.style.left).toBe("80px")
    expect(el.style.top).toBe("120px")
    expect(el.style.width).toBe("160px")
    expect(el.style.height).toBe("120px")
    expect(el.style.transition).toContain(
      `${OUTLINE_SELECTION_BBOX_HIGHLIGHT_FADE_MS}ms`
    )
    expect(el.style.opacity).toBe("1")
    vi.advanceTimersByTime(0)
    expect(el.style.opacity).toBe("0")
    vi.advanceTimersByTime(OUTLINE_SELECTION_BBOX_HIGHLIGHT_FADE_MS)
    expect(
      host.querySelector("[data-testid=outline-selection-bbox-highlight]")
    ).toBeNull()
  })

  it("cancel() removes the highlight and pending timers", () => {
    vi.useFakeTimers()
    const cancel = attachOutlineSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 100,
      height: 100,
    })
    cancel()
    expect(
      host.querySelector("[data-testid=outline-selection-bbox-highlight]")
    ).toBeNull()
    vi.advanceTimersByTime(OUTLINE_SELECTION_BBOX_HIGHLIGHT_FADE_MS)
    expect(
      host.querySelector("[data-testid=outline-selection-bbox-highlight]")
    ).toBeNull()
  })
})

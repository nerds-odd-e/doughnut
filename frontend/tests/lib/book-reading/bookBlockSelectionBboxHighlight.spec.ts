import {
  attachBookBlockSelectionBboxHighlight,
  BOOK_BLOCK_SELECTION_BBOX_HIGHLIGHT_FADE_MS,
} from "@/lib/book-reading/bookBlockSelectionBboxHighlight"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("attachBookBlockSelectionBboxHighlight", () => {
  let host: HTMLDivElement

  beforeEach(() => {
    host = document.createElement("div")
    document.body.appendChild(host)
  })

  afterEach(() => {
    host.remove()
  })

  it("places overlay at the given pixel rect", () => {
    attachBookBlockSelectionBboxHighlight(host, {
      left: 80,
      top: 120,
      width: 160,
      height: 120,
    })
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.style.left).toBe("80px")
    expect(el.style.top).toBe("120px")
    expect(el.style.width).toBe("160px")
    expect(el.style.height).toBe("120px")
    expect(el).not.toBeNull()
  })

  it("always sets pointerEvents to none (hit-testing is handled by the viewer container)", () => {
    attachBookBlockSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 10,
      height: 10,
      contentBlockId: 42,
    })
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.style.pointerEvents).toBe("none")
  })

  it("sets data-book-content-block-id when contentBlockId is provided", () => {
    attachBookBlockSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 10,
      height: 10,
      contentBlockId: 42,
    })
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.dataset.bookContentBlockId).toBe("42")
  })

  it("does not set data-book-content-block-id when contentBlockId is absent", () => {
    attachBookBlockSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 10,
      height: 10,
    })
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.dataset.bookContentBlockId).toBeUndefined()
  })

  it("sets data-derived-title-truncated when derivedTitle is at max length (>=512)", () => {
    const longTitle = "a".repeat(512)
    attachBookBlockSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 10,
      height: 10,
      contentBlockId: 42,
      derivedTitle: longTitle,
    })
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.dataset.derivedTitleTruncated).toBe("true")
  })

  it("does not set data-derived-title-truncated when derivedTitle is short", () => {
    attachBookBlockSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 10,
      height: 10,
      contentBlockId: 42,
      derivedTitle: "short title",
    })
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.dataset.derivedTitleTruncated).toBeUndefined()
  })

  it("fades out overlay after fade timeout", () => {
    vi.useFakeTimers()
    attachBookBlockSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 10,
      height: 10,
    })
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el).not.toBeNull()

    vi.advanceTimersByTime(1)
    expect(el.style.opacity).toBe("0")

    vi.advanceTimersByTime(BOOK_BLOCK_SELECTION_BBOX_HIGHLIGHT_FADE_MS)
    expect(
      host.querySelector("[data-testid=book-block-selection-bbox-highlight]")
    ).toBeNull()
    vi.useRealTimers()
  })

  it("allows multiple simultaneous overlays", () => {
    attachBookBlockSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 10,
      height: 10,
    })
    attachBookBlockSelectionBboxHighlight(host, {
      left: 20,
      top: 0,
      width: 10,
      height: 10,
    })
    expect(
      host.querySelectorAll("[data-testid=book-block-selection-bbox-highlight]")
        .length
    ).toBe(2)
  })

  it("cancel() removes the overlay", () => {
    const cancel = attachBookBlockSelectionBboxHighlight(host, {
      left: 0,
      top: 0,
      width: 100,
      height: 100,
    })
    cancel()
    expect(
      host.querySelector("[data-testid=book-block-selection-bbox-highlight]")
    ).toBeNull()
  })
})

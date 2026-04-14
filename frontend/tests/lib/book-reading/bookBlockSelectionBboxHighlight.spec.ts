import { attachBookBlockSelectionBboxHighlight } from "@/lib/book-reading/bookBlockSelectionBboxHighlight"
import { afterEach, beforeEach, describe, expect, it } from "vitest"

describe("attachBookBlockSelectionBboxHighlight", () => {
  let host: HTMLDivElement

  beforeEach(() => {
    host = document.createElement("div")
    document.body.appendChild(host)
  })

  afterEach(() => {
    host.remove()
  })

  it("places overlay at the given pixel rect and keeps it visible", () => {
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
    expect(el.style.pointerEvents).toBe("auto")
    expect(el).not.toBeNull()
  })

  it("sets data-book-content-block-id when contentBlockId is provided", () => {
    attachBookBlockSelectionBboxHighlight(
      host,
      { left: 0, top: 0, width: 10, height: 10 },
      42
    )
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

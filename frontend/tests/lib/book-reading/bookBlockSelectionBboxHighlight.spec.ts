import { attachBookBlockSelectionBboxHighlight } from "@/lib/book-reading/bookBlockSelectionBboxHighlight"
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

  it("sets data-derived-title-truncated when derivedTitle is at max length (>=512)", () => {
    const longTitle = "a".repeat(512)
    attachBookBlockSelectionBboxHighlight(
      host,
      { left: 0, top: 0, width: 10, height: 10 },
      42,
      undefined,
      longTitle
    )
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.dataset.derivedTitleTruncated).toBe("true")
  })

  it("does not set data-derived-title-truncated when derivedTitle is short", () => {
    attachBookBlockSelectionBboxHighlight(
      host,
      { left: 0, top: 0, width: 10, height: 10 },
      42,
      undefined,
      "short title"
    )
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    expect(el.dataset.derivedTitleTruncated).toBeUndefined()
  })

  it("passes derivedTitle to onLongPress callback", () => {
    const onLongPress = vi.fn()
    const title = "a".repeat(512)
    attachBookBlockSelectionBboxHighlight(
      host,
      { left: 0, top: 0, width: 100, height: 100 },
      7,
      onLongPress,
      title
    )
    const el = host.querySelector(
      "[data-testid=book-block-selection-bbox-highlight]"
    ) as HTMLElement
    vi.useFakeTimers()
    el.dispatchEvent(
      new PointerEvent("pointerdown", {
        clientX: 50,
        clientY: 50,
        bubbles: true,
      })
    )
    vi.advanceTimersByTime(501)
    expect(onLongPress).toHaveBeenCalledWith(7, 50, 50, title)
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

  describe("long-press detection", () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it("calls onLongPress after 500ms hold with no significant movement", () => {
      const onLongPress = vi.fn()
      attachBookBlockSelectionBboxHighlight(
        host,
        { left: 0, top: 0, width: 100, height: 100 },
        7,
        onLongPress
      )
      const el = host.querySelector(
        "[data-testid=book-block-selection-bbox-highlight]"
      ) as HTMLElement
      el.dispatchEvent(
        new PointerEvent("pointerdown", {
          clientX: 50,
          clientY: 50,
          bubbles: true,
        })
      )
      vi.advanceTimersByTime(499)
      expect(onLongPress).not.toHaveBeenCalled()
      vi.advanceTimersByTime(2)
      expect(onLongPress).toHaveBeenCalledWith(7, 50, 50, undefined)
    })

    it("does not call onLongPress if pointerup fires before threshold", () => {
      const onLongPress = vi.fn()
      attachBookBlockSelectionBboxHighlight(
        host,
        { left: 0, top: 0, width: 100, height: 100 },
        7,
        onLongPress
      )
      const el = host.querySelector(
        "[data-testid=book-block-selection-bbox-highlight]"
      ) as HTMLElement
      el.dispatchEvent(
        new PointerEvent("pointerdown", {
          clientX: 50,
          clientY: 50,
          bubbles: true,
        })
      )
      el.dispatchEvent(new PointerEvent("pointerup", { bubbles: true }))
      vi.advanceTimersByTime(600)
      expect(onLongPress).not.toHaveBeenCalled()
    })

    it("does not call onLongPress if pointer moves beyond tolerance", () => {
      const onLongPress = vi.fn()
      attachBookBlockSelectionBboxHighlight(
        host,
        { left: 0, top: 0, width: 100, height: 100 },
        7,
        onLongPress
      )
      const el = host.querySelector(
        "[data-testid=book-block-selection-bbox-highlight]"
      ) as HTMLElement
      el.dispatchEvent(
        new PointerEvent("pointerdown", {
          clientX: 50,
          clientY: 50,
          bubbles: true,
        })
      )
      el.dispatchEvent(
        new PointerEvent("pointermove", {
          clientX: 65,
          clientY: 50,
          bubbles: true,
        })
      )
      vi.advanceTimersByTime(600)
      expect(onLongPress).not.toHaveBeenCalled()
    })

    it("cancel() clears any pending hold timer", () => {
      const onLongPress = vi.fn()
      const cancel = attachBookBlockSelectionBboxHighlight(
        host,
        { left: 0, top: 0, width: 100, height: 100 },
        7,
        onLongPress
      )
      const el = host.querySelector(
        "[data-testid=book-block-selection-bbox-highlight]"
      ) as HTMLElement
      el.dispatchEvent(
        new PointerEvent("pointerdown", {
          clientX: 50,
          clientY: 50,
          bubbles: true,
        })
      )
      cancel()
      vi.advanceTimersByTime(600)
      expect(onLongPress).not.toHaveBeenCalled()
    })
  })
})

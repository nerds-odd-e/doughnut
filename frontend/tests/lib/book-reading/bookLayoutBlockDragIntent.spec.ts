import {
  BOOK_LAYOUT_BLOCK_DRAG_THRESHOLD_PX,
  bookLayoutBlockDragIntent,
  bookLayoutBlockDragShouldCapture,
} from "@/lib/book-reading/bookLayoutBlockDragIntent"
import { describe, expect, it } from "vitest"

const opts = { thresholdPx: BOOK_LAYOUT_BLOCK_DRAG_THRESHOLD_PX }

describe("bookLayoutBlockDragIntent", () => {
  it("returns NONE when movement is below threshold", () => {
    expect(bookLayoutBlockDragIntent(23, 0, opts)).toBe("NONE")
    expect(bookLayoutBlockDragIntent(-23, 0, opts)).toBe("NONE")
  })

  it("returns INDENT when dragging right past threshold with horizontal dominance", () => {
    expect(bookLayoutBlockDragIntent(24, 0, opts)).toBe("INDENT")
    expect(bookLayoutBlockDragIntent(24, 23, opts)).toBe("INDENT")
  })

  it("returns OUTDENT when dragging left past threshold with horizontal dominance", () => {
    expect(bookLayoutBlockDragIntent(-24, 0, opts)).toBe("OUTDENT")
    expect(bookLayoutBlockDragIntent(-24, -23, opts)).toBe("OUTDENT")
  })

  it("returns NONE when vertical movement dominates or ties", () => {
    expect(bookLayoutBlockDragIntent(24, 24, opts)).toBe("NONE")
    expect(bookLayoutBlockDragIntent(30, 40, opts)).toBe("NONE")
    expect(bookLayoutBlockDragIntent(-30, -40, opts)).toBe("NONE")
  })

  it("respects custom threshold", () => {
    const t = { thresholdPx: 10 }
    expect(bookLayoutBlockDragIntent(10, 0, t)).toBe("INDENT")
    expect(bookLayoutBlockDragIntent(9, 0, t)).toBe("NONE")
  })
})

describe("bookLayoutBlockDragShouldCapture", () => {
  it("matches threshold and dominance used for intent", () => {
    expect(bookLayoutBlockDragShouldCapture(24, 23, opts)).toBe(true)
    expect(bookLayoutBlockDragShouldCapture(24, 24, opts)).toBe(false)
    expect(bookLayoutBlockDragShouldCapture(23, 0, opts)).toBe(false)
  })
})

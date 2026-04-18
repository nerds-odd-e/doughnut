import {
  BOOK_READING_LAYOUT_BREAKPOINT_PX,
  bookLayoutAsideInitiallyOpen,
} from "@/lib/book-reading/bookReadingLayoutBreakpoint"
import { describe, expect, it } from "vitest"

describe("bookLayoutAsideInitiallyOpen", () => {
  it("is open at the layout breakpoint and above", () => {
    expect(
      bookLayoutAsideInitiallyOpen(BOOK_READING_LAYOUT_BREAKPOINT_PX)
    ).toBe(true)
    expect(
      bookLayoutAsideInitiallyOpen(BOOK_READING_LAYOUT_BREAKPOINT_PX + 1)
    ).toBe(true)
    expect(bookLayoutAsideInitiallyOpen(1200)).toBe(true)
  })

  it("is closed below the layout breakpoint", () => {
    expect(
      bookLayoutAsideInitiallyOpen(BOOK_READING_LAYOUT_BREAKPOINT_PX - 1)
    ).toBe(false)
    expect(bookLayoutAsideInitiallyOpen(320)).toBe(false)
  })
})

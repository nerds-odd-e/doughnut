import type { BookBlockEpubStartRow } from "@/lib/book-reading/currentBlockIdFromEpubView"
import { currentBlockIdFromEpubView } from "@/lib/book-reading/currentBlockIdFromEpubView"
import type { EpubLocatorFull } from "@generated/donut-backend-api"
import { describe, expect, it } from "vitest"

function row(id: number, fragment?: string): BookBlockEpubStartRow {
  const start: EpubLocatorFull = {
    type: "EpubLocator_Full",
    href: "OEBPS/ch.xhtml",
    ...(fragment !== undefined ? { fragment } : {}),
  }
  return { id, contentLocators: [start] }
}

const blocks = [row(1), row(2, "a"), row(3, "b"), row(4, "c")]

function currentBlock(
  startTops: Record<string, number>,
  selectedBlockId: number | null = null,
  landingLimitPx = 0
) {
  return currentBlockIdFromEpubView(
    blocks,
    {
      startTopPx: (start) => startTops[start.fragment ?? ""] ?? null,
      landingLimitPx,
    },
    selectedBlockId
  )
}

describe("currentBlockIdFromEpubView", () => {
  it("is the last block whose start is at or above the top of the view", () => {
    expect(currentBlock({ "": -900, a: -40, b: 120, c: 600 })).toBe(2)
    expect(currentBlock({ "": -900, a: -40, b: 0, c: 600 })).toBe(3)
  })

  it("counts a start a heading margin below the top as at the top", () => {
    expect(currentBlock({ "": -900, a: 20, b: 300, c: 600 })).toBe(2)
    expect(currentBlock({ "": -900, a: 40, b: 300, c: 600 })).toBe(1)
  })

  it("counts starts in sections rendered above the view", () => {
    expect(
      currentBlock({ "": Number.NEGATIVE_INFINITY, a: 30, b: 300, c: 600 })
    ).toBe(1)
  })

  it("keeps the selected block when another block starts at the same place", () => {
    expect(currentBlock({ "": -900, a: 0, b: 0, c: 600 })).toBe(3)
    expect(currentBlock({ "": -900, a: 0, b: 0, c: 600 }, 2)).toBe(2)
  })

  it("moves on from the selected block once another block's start passes the top", () => {
    expect(currentBlock({ "": -900, a: -400, b: -10, c: 600 }, 2)).toBe(3)
  })

  it("keeps a selected block that the end of the book stops short of the top", () => {
    const atEnd = { "": -900, a: -40, b: 200, c: 380 }
    expect(currentBlock(atEnd, 4, 700)).toBe(4)
    expect(currentBlock(atEnd, 4)).toBe(2)
  })

  it("skips blocks whose start cannot be placed", () => {
    const withoutStart: BookBlockEpubStartRow = { id: 9, contentLocators: [] }
    expect(
      currentBlockIdFromEpubView(
        [...blocks, withoutStart],
        { startTopPx: () => -10, landingLimitPx: 0 },
        9
      )
    ).toBe(4)
  })
})

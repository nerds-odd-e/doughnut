import { describe, expect, it } from "vitest"
import { folderIdsWithRowAboveScrollportTop } from "./sidebarActivePathRowsAboveScrollport"

describe("folderIdsWithRowAboveScrollportTop", () => {
  it("returns ids whose row top is strictly above the scrollport top", () => {
    const scrollTop = 100
    const tops: Record<number, number | null> = {
      1: 50,
      2: 99,
      3: 100,
      4: 120,
    }
    expect(
      folderIdsWithRowAboveScrollportTop(
        scrollTop,
        [1, 2, 3, 4],
        (id) => tops[id] ?? null
      )
    ).toEqual([1, 2])
  })

  it("preserves path order and skips missing rows", () => {
    const scrollTop = 0
    const tops: Record<number, number | null | undefined> = {
      10: -5,
      11: undefined,
      12: 10,
    }
    expect(
      folderIdsWithRowAboveScrollportTop(
        scrollTop,
        [10, 11, 12],
        (id) => tops[id] ?? null
      )
    ).toEqual([10])
  })

  it("returns empty when no ids or none above", () => {
    expect(folderIdsWithRowAboveScrollportTop(0, [], () => 0)).toEqual([])
    expect(folderIdsWithRowAboveScrollportTop(0, [1, 2], () => 5)).toEqual([])
  })
})

import { predecessorBookBlockIdInPreorder } from "@/lib/book-reading/predecessorBookBlockIdInPreorder"
import { describe, expect, it } from "vitest"

describe("predecessorBookBlockIdInPreorder", () => {
  it("returns the previous block id in preorder when canceling a non-first block", () => {
    const blocks = [{ id: 10 }, { id: 20 }, { id: 30 }]
    expect(predecessorBookBlockIdInPreorder(blocks, 20)).toBe(10)
    expect(predecessorBookBlockIdInPreorder(blocks, 30)).toBe(20)
  })

  it("returns null for the first block", () => {
    const blocks = [{ id: 10 }, { id: 20 }]
    expect(predecessorBookBlockIdInPreorder(blocks, 10)).toBeNull()
  })

  it("returns null when the canceled id is not in the list", () => {
    expect(
      predecessorBookBlockIdInPreorder([{ id: 1 }, { id: 2 }], 99)
    ).toBeNull()
  })

  it("returns null for an empty layout", () => {
    expect(predecessorBookBlockIdInPreorder([], 1)).toBeNull()
  })
})

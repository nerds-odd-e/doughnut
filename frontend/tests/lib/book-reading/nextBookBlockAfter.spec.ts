import { nextBookBlockAfter } from "@/lib/book-reading/nextBookBlockAfter"
import { describe, expect, it } from "vitest"

describe("nextBookBlockAfter", () => {
  it("returns the next block in preorder for a middle block", () => {
    const blocks = [{ id: 10 }, { id: 20 }, { id: 30 }]
    expect(nextBookBlockAfter(blocks, 10)).toEqual({ id: 20 })
    expect(nextBookBlockAfter(blocks, 20)).toEqual({ id: 30 })
  })

  it("returns null for the last block", () => {
    const blocks = [{ id: 10 }, { id: 20 }]
    expect(nextBookBlockAfter(blocks, 20)).toBeNull()
  })

  it("returns null when the given id is not in the list", () => {
    expect(nextBookBlockAfter([{ id: 1 }, { id: 2 }], 99)).toBeNull()
  })

  it("returns null for an empty layout", () => {
    expect(nextBookBlockAfter([], 1)).toBeNull()
  })
})

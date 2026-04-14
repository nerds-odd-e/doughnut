import {
  BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS,
  defaultStructuralTitleDraft,
  structuralTitleSourceFromContentBlockRaw,
} from "@/lib/book-reading/contentBlockStructuralTitleSource"
import { describe, expect, it } from "vitest"

describe("structuralTitleSourceFromContentBlockRaw", () => {
  it("returns exceedsMax false when text is within limit", () => {
    const raw = JSON.stringify({ text: "x".repeat(512) })
    const r = structuralTitleSourceFromContentBlockRaw(raw)
    expect(r.fullText).toHaveLength(512)
    expect(r.exceedsMax).toBe(false)
  })

  it("returns exceedsMax true when trimmed text is longer than limit", () => {
    const raw = JSON.stringify({ text: "y".repeat(513) })
    const r = structuralTitleSourceFromContentBlockRaw(raw)
    expect(r.fullText).toHaveLength(513)
    expect(r.exceedsMax).toBe(true)
  })
})

describe("defaultStructuralTitleDraft", () => {
  it("truncates to max chars", () => {
    const s = "a".repeat(520)
    expect(defaultStructuralTitleDraft(s)).toHaveLength(
      BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS
    )
  })
})

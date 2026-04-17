import { currentBlockIdFromEpubLocation } from "@/lib/book-reading/currentBlockIdFromEpubLocation"
import { describe, expect, it } from "vitest"

describe("currentBlockIdFromEpubLocation", () => {
  it("returns the block id when the relocated href exactly matches a single block path", () => {
    const blocks = [{ id: 10, epubStartHref: "OEBPS/chapter1.xhtml" }]
    expect(currentBlockIdFromEpubLocation(blocks, "OEBPS/chapter1.xhtml")).toBe(
      10
    )
  })

  it("matches epub.js manifest-relative href to stored package-root path", () => {
    const blocks = [{ id: 10, epubStartHref: "OEBPS/chapter1.xhtml" }]
    expect(currentBlockIdFromEpubLocation(blocks, "chapter1.xhtml")).toBe(10)
  })

  it("returns the last block in preorder when the same spine path appears with different fragments", () => {
    const blocks = [
      { id: 1, epubStartHref: "OEBPS/chapter2.xhtml#part-one" },
      { id: 2, epubStartHref: "OEBPS/chapter2.xhtml#section-beta" },
    ]
    expect(currentBlockIdFromEpubLocation(blocks, "OEBPS/chapter2.xhtml")).toBe(
      2
    )
  })

  it("returns null when no block path matches", () => {
    const blocks = [{ id: 1, epubStartHref: "OEBPS/a.xhtml" }]
    expect(currentBlockIdFromEpubLocation(blocks, "OEBPS/b.xhtml")).toBe(null)
  })

  it("skips blocks without epubStartHref", () => {
    const blocks = [
      { id: 1, title: "x" },
      { id: 2, epubStartHref: "OEBPS/only.xhtml" },
    ] as Array<{ id: number; epubStartHref?: string; title?: string }>
    expect(currentBlockIdFromEpubLocation(blocks, "OEBPS/only.xhtml")).toBe(2)
  })

  it("prefers the last fragment match in preorder when the relocated href includes a fragment", () => {
    const blocks = [
      { id: 1, epubStartHref: "OEBPS/ch.xhtml" },
      { id: 2, epubStartHref: "OEBPS/ch.xhtml#early" },
      { id: 3, epubStartHref: "OEBPS/ch.xhtml#early" },
      { id: 4, epubStartHref: "OEBPS/ch.xhtml#target" },
    ]
    expect(
      currentBlockIdFromEpubLocation(blocks, "OEBPS/ch.xhtml#target")
    ).toBe(4)
  })

  it("falls back to last path match when the relocated fragment does not match any block", () => {
    const blocks = [
      { id: 1, epubStartHref: "OEBPS/ch.xhtml#a" },
      { id: 2, epubStartHref: "OEBPS/ch.xhtml#b" },
    ]
    expect(
      currentBlockIdFromEpubLocation(blocks, "OEBPS/ch.xhtml#unknown")
    ).toBe(2)
  })

  it("returns null for empty href", () => {
    const blocks = [{ id: 1, epubStartHref: "OEBPS/a.xhtml" }]
    expect(currentBlockIdFromEpubLocation(blocks, "")).toBe(null)
  })
})

import type { BookBlockEpubLocationRow } from "@/lib/book-reading/currentBlockIdFromEpubLocation"
import { currentBlockIdFromEpubLocation } from "@/lib/book-reading/currentBlockIdFromEpubLocation"
import type { EpubLocatorFull } from "@generated/doughnut-backend-api"
import { describe, expect, it } from "vitest"

function epubLoc(href: string, fragment?: string): EpubLocatorFull {
  return {
    type: "EpubLocator_Full",
    href,
    ...(fragment !== undefined ? { fragment } : {}),
  }
}

function row(id: number, ...locs: EpubLocatorFull[]): BookBlockEpubLocationRow {
  return { id, contentLocators: locs }
}

describe("currentBlockIdFromEpubLocation", () => {
  it("returns the block id when the relocated href exactly matches a single block path", () => {
    const blocks = [row(10, epubLoc("OEBPS/chapter1.xhtml"))]
    expect(currentBlockIdFromEpubLocation(blocks, "OEBPS/chapter1.xhtml")).toBe(
      10
    )
  })

  it("matches epub.js manifest-relative href to stored package-root path", () => {
    const blocks = [row(10, epubLoc("OEBPS/chapter1.xhtml"))]
    expect(currentBlockIdFromEpubLocation(blocks, "chapter1.xhtml")).toBe(10)
  })

  it("returns the last block in preorder when the same spine path appears with different fragments", () => {
    const blocks = [
      row(1, epubLoc("OEBPS/chapter2.xhtml", "part-one")),
      row(2, epubLoc("OEBPS/chapter2.xhtml", "section-beta")),
    ]
    expect(currentBlockIdFromEpubLocation(blocks, "OEBPS/chapter2.xhtml")).toBe(
      2
    )
  })

  it("returns null when no block path matches", () => {
    const blocks = [row(1, epubLoc("OEBPS/a.xhtml"))]
    expect(currentBlockIdFromEpubLocation(blocks, "OEBPS/b.xhtml")).toBe(null)
  })

  it("skips blocks with no EPUB block-start locator", () => {
    const blocks: BookBlockEpubLocationRow[] = [
      { id: 1, contentLocators: [] },
      row(2, epubLoc("OEBPS/only.xhtml")),
    ]
    expect(currentBlockIdFromEpubLocation(blocks, "OEBPS/only.xhtml")).toBe(2)
  })

  it("prefers the last fragment match in preorder when the relocated href includes a fragment", () => {
    const blocks = [
      row(1, epubLoc("OEBPS/ch.xhtml")),
      row(2, epubLoc("OEBPS/ch.xhtml", "early")),
      row(3, epubLoc("OEBPS/ch.xhtml", "early")),
      row(4, epubLoc("OEBPS/ch.xhtml", "target")),
    ]
    expect(
      currentBlockIdFromEpubLocation(blocks, "OEBPS/ch.xhtml#target")
    ).toBe(4)
  })

  it("falls back to last path match when the relocated fragment does not match any block", () => {
    const blocks = [
      row(1, epubLoc("OEBPS/ch.xhtml", "a")),
      row(2, epubLoc("OEBPS/ch.xhtml", "b")),
    ]
    expect(
      currentBlockIdFromEpubLocation(blocks, "OEBPS/ch.xhtml#unknown")
    ).toBe(2)
  })

  it("returns null for empty href", () => {
    const blocks = [row(1, epubLoc("OEBPS/a.xhtml"))]
    expect(currentBlockIdFromEpubLocation(blocks, "")).toBe(null)
  })
})

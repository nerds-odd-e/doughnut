import {
  createLastReadPositionPatchDebouncer,
  type LastReadPositionPatchBody,
} from "@/lib/book-reading/debounceLastReadPositionPatch"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

function epubLoc(href: string, fragment?: string) {
  return fragment === undefined
    ? { type: "EpubLocator_Full" as const, href }
    : { type: "EpubLocator_Full" as const, href, fragment }
}

function pdfLoc(pageIndex: number, normalizedY: number) {
  const y = Math.max(0, Math.min(1000, normalizedY))
  return {
    type: "PdfLocator_Full" as const,
    pageIndex,
    bbox: [0, y, 0, y],
  }
}

describe("createLastReadPositionPatchDebouncer", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  function setup() {
    const sent: LastReadPositionPatchBody[] = []
    const d = createLastReadPositionPatchDebouncer({
      delayMs: 100,
      patch: (body) => {
        sent.push(body)
        return Promise.resolve()
      },
    })
    return { d, sent }
  }

  describe("EPUB branch", () => {
    it("debounces rapid epub proposes into one PATCH with the latest body", () => {
      const { d, sent } = setup()
      d.propose(epubLoc("OEBPS/ch1.xhtml"))
      d.propose(epubLoc("OEBPS/ch2.xhtml"))
      d.propose(epubLoc("OEBPS/ch3.xhtml", "sec"))
      expect(sent).toEqual([])
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([{ locator: epubLoc("OEBPS/ch3.xhtml", "sec") }])
    })

    it("includes selectedBookBlockId when supplied", () => {
      const { d, sent } = setup()
      d.propose(epubLoc("OEBPS/ch1.xhtml", "a"), 42)
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        {
          locator: epubLoc("OEBPS/ch1.xhtml", "a"),
          selectedBookBlockId: 42,
        },
      ])
    })

    it("dedupes the second identical epub propose after a successful send", async () => {
      const { d, sent } = setup()
      d.propose(epubLoc("OEBPS/ch1.xhtml"), 7)
      vi.advanceTimersByTime(100)
      await Promise.resolve()
      d.propose(epubLoc("OEBPS/ch1.xhtml"), 7)
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        {
          locator: epubLoc("OEBPS/ch1.xhtml"),
          selectedBookBlockId: 7,
        },
      ])
    })

    it("treats different selectedBookBlockId as a distinct body", async () => {
      const { d, sent } = setup()
      d.propose(epubLoc("OEBPS/ch1.xhtml"), 1)
      vi.advanceTimersByTime(100)
      await Promise.resolve()
      d.propose(epubLoc("OEBPS/ch1.xhtml"), 2)
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        { locator: epubLoc("OEBPS/ch1.xhtml"), selectedBookBlockId: 1 },
        { locator: epubLoc("OEBPS/ch1.xhtml"), selectedBookBlockId: 2 },
      ])
    })

    it("treats epub and pdf bodies as distinct (no dedupe across variants)", async () => {
      const { d, sent } = setup()
      d.propose(pdfLoc(1, 500))
      vi.advanceTimersByTime(100)
      await Promise.resolve()
      d.propose(epubLoc("OEBPS/ch1.xhtml"))
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        { locator: pdfLoc(1, 500) },
        { locator: epubLoc("OEBPS/ch1.xhtml") },
      ])
    })

    it("cancel() drops a pending epub propose", () => {
      const { d, sent } = setup()
      d.propose(epubLoc("OEBPS/ch1.xhtml"))
      d.cancel()
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([])
    })

    it("swallows patch rejections and continues to allow further sends", async () => {
      const sent: LastReadPositionPatchBody[] = []
      let shouldFail = true
      const d = createLastReadPositionPatchDebouncer({
        delayMs: 100,
        patch: (body) => {
          sent.push(body)
          if (shouldFail) {
            return Promise.reject(new Error("boom"))
          }
          return Promise.resolve()
        },
      })

      d.propose(epubLoc("OEBPS/ch1.xhtml"))
      vi.advanceTimersByTime(100)
      await Promise.resolve()
      await Promise.resolve()

      shouldFail = false
      d.propose(epubLoc("OEBPS/ch2.xhtml"))
      vi.advanceTimersByTime(100)
      await Promise.resolve()

      expect(sent).toEqual([
        { locator: epubLoc("OEBPS/ch1.xhtml") },
        { locator: epubLoc("OEBPS/ch2.xhtml") },
      ])
    })
  })

  describe("PDF branch (existing behavior)", () => {
    it("debounces rapid pdf proposes and sends the last", () => {
      const { d, sent } = setup()
      d.propose(pdfLoc(1, 100))
      d.propose(pdfLoc(1, 200))
      d.propose(pdfLoc(2, 300), 5)
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        {
          locator: pdfLoc(2, 300),
          selectedBookBlockId: 5,
        },
      ])
    })
  })
})

import {
  createLastReadPositionPatchDebouncer,
  type LastReadPositionPatchBody,
} from "@/lib/book-reading/debounceLastReadPositionPatch"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

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
      d.proposeEpubLocator("OEBPS/ch1.xhtml")
      d.proposeEpubLocator("OEBPS/ch2.xhtml")
      d.proposeEpubLocator("OEBPS/ch3.xhtml#sec")
      expect(sent).toEqual([])
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([{ epubLocator: "OEBPS/ch3.xhtml#sec" }])
    })

    it("includes selectedBookBlockId when supplied", () => {
      const { d, sent } = setup()
      d.proposeEpubLocator("OEBPS/ch1.xhtml#a", 42)
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        { epubLocator: "OEBPS/ch1.xhtml#a", selectedBookBlockId: 42 },
      ])
    })

    it("dedupes the second identical epub propose after a successful send", async () => {
      const { d, sent } = setup()
      d.proposeEpubLocator("OEBPS/ch1.xhtml", 7)
      vi.advanceTimersByTime(100)
      await Promise.resolve()
      d.proposeEpubLocator("OEBPS/ch1.xhtml", 7)
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        { epubLocator: "OEBPS/ch1.xhtml", selectedBookBlockId: 7 },
      ])
    })

    it("treats different selectedBookBlockId as a distinct body", async () => {
      const { d, sent } = setup()
      d.proposeEpubLocator("OEBPS/ch1.xhtml", 1)
      vi.advanceTimersByTime(100)
      await Promise.resolve()
      d.proposeEpubLocator("OEBPS/ch1.xhtml", 2)
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        { epubLocator: "OEBPS/ch1.xhtml", selectedBookBlockId: 1 },
        { epubLocator: "OEBPS/ch1.xhtml", selectedBookBlockId: 2 },
      ])
    })

    it("treats epub and pdf bodies as distinct (no dedupe across variants)", async () => {
      const { d, sent } = setup()
      d.propose(1, 0.5)
      vi.advanceTimersByTime(100)
      await Promise.resolve()
      d.proposeEpubLocator("OEBPS/ch1.xhtml")
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        { pageIndex: 1, normalizedY: 0.5 },
        { epubLocator: "OEBPS/ch1.xhtml" },
      ])
    })

    it("cancel() drops a pending epub propose", () => {
      const { d, sent } = setup()
      d.proposeEpubLocator("OEBPS/ch1.xhtml")
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

      d.proposeEpubLocator("OEBPS/ch1.xhtml")
      vi.advanceTimersByTime(100)
      await Promise.resolve()
      await Promise.resolve()

      shouldFail = false
      d.proposeEpubLocator("OEBPS/ch2.xhtml")
      vi.advanceTimersByTime(100)
      await Promise.resolve()

      expect(sent).toEqual([
        { epubLocator: "OEBPS/ch1.xhtml" },
        { epubLocator: "OEBPS/ch2.xhtml" },
      ])
    })
  })

  describe("PDF branch (existing behavior)", () => {
    it("debounces rapid pdf proposes and sends the last", () => {
      const { d, sent } = setup()
      d.propose(1, 0.1)
      d.propose(1, 0.2)
      d.propose(2, 0.3, 5)
      vi.advanceTimersByTime(100)
      expect(sent).toEqual([
        { pageIndex: 2, normalizedY: 0.3, selectedBookBlockId: 5 },
      ])
    })
  })
})

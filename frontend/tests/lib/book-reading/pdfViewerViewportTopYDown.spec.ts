import { describe, expect, it, vi } from "vitest"
import { pageIndexForScrollContainerCenter } from "@/lib/book-reading/pdfViewerViewportTopYDown"
import type { PDFViewer } from "pdfjs-dist/web/pdf_viewer.mjs"

function rect(top: number, bottom: number) {
  return {
    top,
    bottom,
    left: 0,
    right: 100,
    width: 100,
    height: bottom - top,
    x: 0,
    y: top,
    toJSON: () => ({}),
  }
}

function mockViewer(
  pageRects: Array<{ top: number; bottom: number } | null>,
  currentPageNumber: number
): PDFViewer {
  return {
    pagesCount: pageRects.length,
    currentPageNumber,
    getPageView: (i: number) => {
      const r = pageRects[i]
      if (r == null) {
        return
      }
      return {
        div: {
          getBoundingClientRect: () => rect(r.top, r.bottom),
        } as unknown as HTMLDivElement,
      }
    },
  } as unknown as PDFViewer
}

describe("pageIndexForScrollContainerCenter", () => {
  it("prefers the page that contains the container vertical midpoint over currentPageNumber", () => {
    const scrollContainer = {
      getBoundingClientRect: () => rect(0, 100),
    } as HTMLElement
    const viewer = mockViewer(
      [
        { top: -500, bottom: 20 },
        { top: 30, bottom: 200 },
      ],
      1
    )
    expect(pageIndexForScrollContainerCenter(scrollContainer, viewer)).toBe(1)
  })

  it("when midpoint is in a gap, picks the page with the largest visible overlap", () => {
    const scrollContainer = {
      getBoundingClientRect: () => rect(0, 100),
    } as HTMLElement
    const viewer = mockViewer(
      [
        { top: 0, bottom: 35 },
        { top: 55, bottom: 100 },
      ],
      1
    )
    expect(pageIndexForScrollContainerCenter(scrollContainer, viewer)).toBe(1)
  })

  it("returns 0 when pagesCount is 0", () => {
    const scrollContainer = {
      getBoundingClientRect: vi.fn(),
    } as unknown as HTMLElement
    const viewer = {
      pagesCount: 0,
      currentPageNumber: 1,
      getPageView: vi.fn(),
    } as unknown as PDFViewer
    expect(pageIndexForScrollContainerCenter(scrollContainer, viewer)).toBe(0)
    expect(scrollContainer.getBoundingClientRect).not.toHaveBeenCalled()
  })
})

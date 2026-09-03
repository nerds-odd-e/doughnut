import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import { pdfScrollRestoreSpy } from "./bookReadingPagePdfViewerTestSupport"
import {
  mountPatchDebounceScenario,
  stubReadingPositionSnapshot,
} from "./bookReadingPageReadingPositionTestSupport"
import {
  LAST_READ_POSITION_PATCH_DEBOUNCE_MS,
  bookId,
  getTopMathsPdfBytes,
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
  mockNotebookBookFilePdfOk,
  mountBookReadingPage,
  notebookId,
  stubGetBookPlain,
  stubGetBookWithTopMathsBlocks,
  waitForPdfViewer,
  withFakeTimers,
} from "./bookReadingPageTestSupport"

describe("BookReadingPage reading position", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
  })

  it("debounces PATCH reading position; keeps last top and selected block; skips null viewport", async () => {
    const { wrapper, patchSpy } = await mountPatchDebounceScenario()
    const pdf = wrapper.findComponent(PdfBookViewer)
    const viewport = { top: 200, mid: 500, bottom: 1000 }

    await withFakeTimers(async () => {
      for (let i = 0; i < 3; i++) {
        pdf.vm.$emit("viewportAnchorPage", {
          anchorPageIndexZeroBased: 2,
          viewport,
          pagesCount: 10,
        })
      }
      expect(patchSpy).not.toHaveBeenCalled()
      vi.advanceTimersByTime(LAST_READ_POSITION_PATCH_DEBOUNCE_MS)
      await flushPromises()
    })

    expect(patchSpy).toHaveBeenCalledTimes(1)
    expect(patchSpy).toHaveBeenCalledWith({
      path: { notebook: notebookId },
      body: {
        locator: {
          type: "PdfLocator_Full",
          pageIndex: 2,
          bbox: [0, 200, 0, 200],
        },
        selectedBookBlockId: 101,
      },
    })

    const row = wrapper
      .findAll('[data-testid="book-reading-book-block"]')
      .find((candidate) => candidate.text() === "Section 3")
    expect(row).toBeDefined()
    await row!.trigger("click")
    await flushPromises()

    patchSpy.mockClear()
    await withFakeTimers(async () => {
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 50, mid: 100, bottom: 200 },
        pagesCount: 10,
      })
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: { top: 150, mid: 250, bottom: 300 },
        pagesCount: 10,
      })
      vi.advanceTimersByTime(LAST_READ_POSITION_PATCH_DEBOUNCE_MS)
      await flushPromises()
    })

    expect(patchSpy).toHaveBeenCalledTimes(1)
    expect(patchSpy.mock.calls[0]?.[0]).toEqual({
      path: { notebook: notebookId },
      body: {
        locator: {
          type: "PdfLocator_Full",
          pageIndex: 0,
          bbox: [0, 150, 0, 150],
        },
        selectedBookBlockId: 103,
      },
    })

    patchSpy.mockClear()
    await withFakeTimers(async () => {
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: null,
        pagesCount: 10,
      })
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 1,
        viewport: null,
        pagesCount: 10,
      })
      vi.advanceTimersByTime(LAST_READ_POSITION_PATCH_DEBOUNCE_MS)
      await flushPromises()
    })

    expect(patchSpy).not.toHaveBeenCalled()
  })

  it("restores reading position from stored snapshot on open", async () => {
    stubGetBookPlain(notebookId)
    stubReadingPositionSnapshot({ pageIndex: 2, bboxTop: 750 })
    mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())

    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)

    const { pdf, spy } = pdfScrollRestoreSpy(wrapper)
    pdf.vm.$emit("pagesReady")
    await flushPromises()

    expect(spy).toHaveBeenCalledWith(2, 750)
  })

  it("does not restore reading position when no snapshot exists", async () => {
    stubGetBookPlain(notebookId)
    mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())

    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)

    const { pdf, spy } = pdfScrollRestoreSpy(wrapper)
    pdf.vm.$emit("pagesReady")
    await flushPromises()

    expect(spy).not.toHaveBeenCalled()
  })

  it("restores selected book block from stored reading snapshot", async () => {
    stubGetBookWithTopMathsBlocks(notebookId)
    stubReadingPositionSnapshot({ selectedBookBlockId: 102 })
    mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())

    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)

    expect(wrapper.find('[data-current-selection="true"]').text()).toBe(
      "Section 2"
    )
  })
})

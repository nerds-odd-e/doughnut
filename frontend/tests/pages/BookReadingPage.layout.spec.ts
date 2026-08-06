import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import { flushPromises } from "@vue/test-utils"
import { emitViewportAndSettleCurrentBlock } from "./bookReadingPageInteractionTestSupport"
import {
  CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS,
  bookId,
  getTopMathsPdfBytes,
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
  mockNotebookBookFilePdfOk,
  mountBookReadingPage,
  mountLoadedBookWithBlocks,
  notebookId,
  stubGetBookPlain,
  waitForPdfViewer,
  withFakeTimers,
  withStubbedInnerWidth,
} from "./bookReadingPageTestSupport"

describe("BookReadingPage layout", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
  })

  it("updates current block while book layout drawer is closed", async () => {
    const wrapper = await mountLoadedBookWithBlocks(notebookId, {
      innerWidth: 500,
    })
    expect(
      wrapper
        .find('[data-testid="book-reading-book-layout-toggle"]')
        .attributes("aria-expanded")
    ).toBe("false")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: null,
      pagesCount: 10,
    })

    expect(
      wrapper.find('[data-testid="book-reading-page-indicator"]').text().trim()
    ).toBe("1 / 10")

    const current = wrapper.find('[data-current-block="true"]')
    expect(current.attributes("aria-current")).toBe("location")
    expect(current.text()).toBe("Section 3")
  })

  it("zoom buttons exist with accessible names and page indicator shows via PdfControl", async () => {
    stubGetBookPlain(notebookId)
    mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)
    expect(
      wrapper.find('[data-testid="pdf-zoom-in"]').attributes("aria-label")
    ).toBe("Zoom in")
    expect(
      wrapper.find('[data-testid="pdf-zoom-out"]').attributes("aria-label")
    ).toBe("Zoom out")

    const pdf = wrapper.findComponent(PdfBookViewer)
    await withFakeTimers(async () => {
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: null,
        pagesCount: 5,
      })
      await vi.advanceTimersByTimeAsync(CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS)
      await flushPromises()
    })

    expect(
      wrapper.find('[data-testid="book-reading-page-indicator"]').text().trim()
    ).toBe("1 / 5")
  })

  it("book layout toggle exposes aria-expanded and aria-controls", async () => {
    await withStubbedInnerWidth(1024, async () => {
      stubGetBookPlain(notebookId)
      mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
      const wrapper = mountBookReadingPage(notebookId)
      await waitForPdfViewer(wrapper)

      const toggle = wrapper.find(
        '[data-testid="book-reading-book-layout-toggle"]'
      )
      const aside = wrapper.find(
        '[data-testid="book-reading-book-layout-aside"]'
      )
      expect(aside.attributes("id")).toBe("book-reading-book-layout-panel")
      expect(toggle.attributes("aria-controls")).toBe(
        "book-reading-book-layout-panel"
      )
      expect(toggle.attributes("aria-expanded")).toBe("true")

      await toggle.trigger("click")
      expect(toggle.attributes("aria-expanded")).toBe("false")

      await toggle.trigger("click")
      expect(toggle.attributes("aria-expanded")).toBe("true")
    })
  })
})

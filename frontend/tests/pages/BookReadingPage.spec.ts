import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import { emitViewportAndSettleCurrentBlock } from "./bookReadingPageInteractionTestSupport"
import {
  type BookReadingPageWrapper,
  bookId,
  getEpubMinimalBytes,
  getTopMathsPdfBytes,
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
  mockNotebookBookFileEpubOk,
  mockNotebookBookFilePdfOk,
  mountBookReadingPage,
  mountLoadedBookWithBlocks,
  notebookId,
  stubGetBookPlain,
  waitForEpubViewer,
  waitForPdfViewer,
  withStubbedInnerWidth,
} from "./bookReadingPageTestSupport"

async function mountPlainPdfBook() {
  stubGetBookPlain(notebookId)
  mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  return wrapper
}

const pageIndicatorText = (wrapper: BookReadingPageWrapper) =>
  wrapper.find('[data-testid="book-reading-page-indicator"]').text().trim()

describe("BookReadingPage", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
  })

  describe("loading the book file", () => {
    it("shows fetch error when book file returns an error status", async () => {
      stubGetBookPlain(notebookId)
      vi.spyOn(globalThis, "fetch").mockResolvedValue(
        new Response(null, { status: 404 })
      )

      const wrapper = mountBookReadingPage(notebookId)
      await flushPromises()

      const err = wrapper.find(
        '[data-testid="book-reading-book-file-load-error"]'
      )
      expect(err.exists()).toBe(true)
      expect(err.text()).toBe("Could not load the book file.")
      expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(false)
      expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(
        false
      )
    })

    it("shows loading indicator while PDF is loading, hides it after render", async () => {
      stubGetBookPlain(notebookId)
      let resolveFetch!: (r: Response) => void
      let fetchInit: RequestInit | undefined
      vi.spyOn(globalThis, "fetch").mockImplementation((_input, init) => {
        fetchInit = init
        return new Promise<Response>((resolve) => {
          resolveFetch = resolve
        })
      })

      const wrapper = mountBookReadingPage(notebookId)
      await flushPromises()

      expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(true)
      expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(
        false
      )

      resolveFetch(
        new Response(getTopMathsPdfBytes().slice(0), {
          status: 200,
          headers: { "Content-Type": "application/pdf" },
        })
      )

      await waitForPdfViewer(wrapper)
      expect(fetchInit?.credentials).toBe("same-origin")
      expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(
        true
      )
      expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(false)
    })

    it("loads EPUB into viewer with book title in bar, no PDF viewer", async () => {
      mockSdkService(
        NotebookBooksController,
        "getBook",
        makeMe.aBook
          .id(bookId)
          .notebookId(String(notebookId))
          .format("epub")
          .blocks([])
          .bookName("My EPUB")
          .please()
      )
      mockNotebookBookFileEpubOk(bookId, getEpubMinimalBytes())

      const wrapper = mountBookReadingPage(notebookId)
      await waitForEpubViewer(wrapper)

      expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(
        false
      )
      expect(
        wrapper
          .find('[data-testid="book-reading-epub-global-bar-title"]')
          .text()
      ).toBe("My EPUB")
    })

    it("shows error when PDF viewer reports invalid PDF", async () => {
      const wrapper = await mountPlainPdfBook()

      wrapper
        .findComponent(PdfBookViewer)
        .vm.$emit("loadError", "This file is not a valid PDF.")
      await flushPromises()

      expect(
        wrapper
          .find('[data-testid="book-reading-pdf-viewer-load-error"]')
          .text()
      ).toBe("This file is not a valid PDF.")
      expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(
        false
      )
    })
  })

  describe("layout", () => {
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

      expect(pageIndicatorText(wrapper)).toBe("1 / 10")

      const current = wrapper.find('[data-current-block="true"]')
      expect(current.attributes("aria-current")).toBe("location")
      expect(current.text()).toBe("Section 3")
    })

    it("zoom buttons exist with accessible names and page indicator shows via PdfControl", async () => {
      const wrapper = await mountPlainPdfBook()
      expect(
        wrapper.find('[data-testid="pdf-zoom-in"]').attributes("aria-label")
      ).toBe("Zoom in")
      expect(
        wrapper.find('[data-testid="pdf-zoom-out"]').attributes("aria-label")
      ).toBe("Zoom out")

      await emitViewportAndSettleCurrentBlock(wrapper, {
        anchorPageIndexZeroBased: 0,
        viewport: null,
        pagesCount: 5,
      })

      expect(pageIndicatorText(wrapper)).toBe("1 / 5")
    })

    it("book layout toggle exposes aria-expanded and aria-controls", async () => {
      await withStubbedInnerWidth(1024, async () => {
        const wrapper = await mountPlainPdfBook()

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
})

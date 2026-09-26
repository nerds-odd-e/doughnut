import CurrentBlockNavigationBar from "@/components/book-reading/CurrentBlockNavigationBar.vue"
import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import {
  clickBookBlockAndExpectSelection,
  emitViewportAndSettleCurrentBlock,
  expectCurrentSelection,
} from "./bookReadingPageInteractionTestSupport"
import {
  pdfScrollRestoreSpy,
  spyOnScrollToBookNavTarget,
} from "./bookReadingPagePdfViewerTestSupport"
import {
  mountPatchDebounceScenario,
  stubReadingPositionSnapshot,
} from "./bookReadingPageReadingPositionTestSupport"
import {
  type BookReadingPageWrapper,
  LAST_READ_POSITION_PATCH_DEBOUNCE_MS,
  bookId,
  getTopMathsPdfBytes,
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
  mockNotebookBookFilePdfOk,
  mountBookReadingPage,
  mountLoadedBookWithBlocks,
  notebookId,
  stubGetBookPlain,
  stubGetBookWithTopMathsBlocks,
  waitForPdfViewer,
  withFakeTimers,
} from "./bookReadingPageTestSupport"

async function mountPlainPdfBookAndReportPagesReady() {
  stubGetBookPlain(notebookId)
  mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  const restore = pdfScrollRestoreSpy(wrapper)
  restore.pdf.vm.$emit("pagesReady")
  await flushPromises()
  return restore
}

async function mountNavBarScenario(viewportMid: number) {
  const wrapper = await mountLoadedBookWithBlocks(notebookId)
  spyOnScrollToBookNavTarget(wrapper)
  await clickBookBlockAndExpectSelection(wrapper, "Section 1")
  await emitViewportAndSettleCurrentBlock(wrapper, {
    anchorPageIndexZeroBased: 0,
    viewport: { top: 0, mid: viewportMid, bottom: 1000 },
    pagesCount: 10,
  })
  return wrapper
}

const currentBlockNavBar = (wrapper: BookReadingPageWrapper) =>
  wrapper.find('[data-testid="current-block-navigation-bar"]')

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
    stubReadingPositionSnapshot({ pageIndex: 2, bboxTop: 750 })
    const { spy } = await mountPlainPdfBookAndReportPagesReady()

    expect(spy).toHaveBeenCalledWith(2, 750)
  })

  it("does not restore reading position when no snapshot exists", async () => {
    const { spy } = await mountPlainPdfBookAndReportPagesReady()

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

  describe("current block navigation bar", () => {
    it("shows navigation bar when current block differs from selected block", async () => {
      const wrapper = await mountNavBarScenario(500)

      expect(currentBlockNavBar(wrapper).exists()).toBe(true)
      expect(currentBlockNavBar(wrapper).text()).toContain("Section 2")
    })

    it("hides navigation bar when current block equals selected block", async () => {
      const wrapper = await mountNavBarScenario(10)

      expect(currentBlockNavBar(wrapper).exists()).toBe(false)
    })

    it("Read from here makes current block the selected block and hides nav bar", async () => {
      const wrapper = await mountNavBarScenario(500)

      await wrapper
        .findComponent(CurrentBlockNavigationBar)
        .vm.$emit("readFromHere")
      await flushPromises()

      expectCurrentSelection(wrapper, "Section 2")
      expect(currentBlockNavBar(wrapper).exists()).toBe(false)
    })

    it("Back to selected scrolls to selected block and hides nav bar", async () => {
      const wrapper = await mountNavBarScenario(500)

      await wrapper
        .findComponent(CurrentBlockNavigationBar)
        .vm.$emit("backToSelected")
      await flushPromises()

      expect(currentBlockNavBar(wrapper).exists()).toBe(false)
      expectCurrentSelection(wrapper, "Section 1")
    })
  })
})

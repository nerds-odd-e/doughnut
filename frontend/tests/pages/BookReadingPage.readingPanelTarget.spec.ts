import makeMe from "donut-test-fixtures/makeMe"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import {
  clickBookBlockAndExpectSelection,
  emitViewportAndSettleCurrentBlock,
  readingControlPanel,
} from "./bookReadingPageInteractionTestSupport"
import {
  mockIsLastContentBottomVisible,
  mockReadingPanelAnchorTopPx,
} from "./bookReadingPagePdfViewerTestSupport"
import {
  bookId,
  getTopMathsPdfBytes,
  loadBookReadingPageFixtures,
  mockBookReadingPageDefaults,
  mockNotebookBookFilePdfOk,
  mountBookReadingPage,
  notebookId,
  stubGetBookWithTopMathsLikeContentLocators,
  waitForPdfViewer,
} from "./bookReadingPageTestSupport"

async function mountFirstBlockWithContentBbox(options?: {
  lastContentBottomVisible?: boolean
}) {
  stubGetBookWithTopMathsLikeContentLocators((i) =>
    i === 0
      ? [
          makeMe.pdfLocator.pageIndexOnly(0),
          makeMe.pdfLocator.withBbox(0, [10, 700, 500, 750]),
        ]
      : [makeMe.bookReading.topMathsLikePreorderFirstLocatorAt(i)]
  )
  mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  mockIsLastContentBottomVisible(
    wrapper,
    options?.lastContentBottomVisible ?? true
  )
  return wrapper
}

describe("BookReadingPage reading panel target", () => {
  beforeAll(async () => {
    await loadBookReadingPageFixtures()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
    mockBookReadingPageDefaults()
  })

  it("anchors panel when last content bottom is visible and anchor px is returned", async () => {
    const wrapper = await mountFirstBlockWithContentBbox()
    mockReadingPanelAnchorTopPx(wrapper, 120)
    await clickBookBlockAndExpectSelection(wrapper, "Section 1")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 200, bottom: 600 },
      pagesCount: 10,
    })

    const panel = readingControlPanel(wrapper)
    expect(panel.exists()).toBe(true)
    expect(panel.attributes("data-panel-placement")).toBe("anchored")
    expect((panel.element as HTMLElement).style.top).toBe("120px")
    expect((panel.element as HTMLElement).style.bottom).toBe("auto")
  })

  it("hides the panel when last content bottom is not yet above obstruction", async () => {
    const wrapper = await mountFirstBlockWithContentBbox({
      lastContentBottomVisible: false,
    })
    await clickBookBlockAndExpectSelection(wrapper, "Section 1")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 200, bottom: 600 },
      pagesCount: 10,
    })

    expect(readingControlPanel(wrapper).exists()).toBe(false)
  })

  it("keeps offering the panel after scrolling past the block's content into its successor", async () => {
    const wrapper = await mountFirstBlockWithContentBbox()
    await clickBookBlockAndExpectSelection(wrapper, "Section 1")

    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 0, mid: 40, bottom: 600 },
      pagesCount: 10,
    })
    expect(readingControlPanel(wrapper).exists()).toBe(true)

    mockIsLastContentBottomVisible(wrapper, false)
    await emitViewportAndSettleCurrentBlock(wrapper, {
      anchorPageIndexZeroBased: 0,
      viewport: { top: 72, mid: 200, bottom: 600 },
      pagesCount: 10,
    })

    expect(wrapper.find('[data-current-block="true"]').text()).toBe("Section 2")
    expect(readingControlPanel(wrapper).exists()).toBe(true)
    expect(
      readingControlPanel(wrapper).attributes("data-panel-placement")
    ).toBe("fixed")
  })
})

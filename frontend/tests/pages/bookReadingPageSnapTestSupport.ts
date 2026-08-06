import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { wrapSdkResponse } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"
import {
  clickBookBlockAndExpectSelection,
  emitViewportAndSettleCurrentBlock,
} from "./bookReadingPageInteractionTestSupport"
import {
  mockIsLastContentBottomVisible,
  mockSnapBackContentFitsInViewport,
} from "./bookReadingPagePdfViewerTestSupport"
import {
  bookId,
  getTopMathsPdfBytes,
  mockNotebookBookFilePdfOk,
  mountBookReadingPage,
  notebookId,
  stubGetBookWithTopMathsLikeContentLocators,
  waitForPdfViewer,
  withFakeTimers,
  type BookReadingPageWrapper,
} from "./bookReadingPageTestSupport"

const firstBlockContentBbox = () =>
  makeMe.pdfLocator.withBbox(0, [10, 700, 500, 750])

function stubGetBookWithFirstBlockHavingBbox() {
  stubGetBookWithTopMathsLikeContentLocators((i) =>
    i === 0
      ? [makeMe.pdfLocator.pageIndexOnly(0), firstBlockContentBbox()]
      : [makeMe.bookReading.topMathsLikePreorderFirstLocatorAt(i)]
  )
}

function stubGetBookWithFirstBlockHavingCrossPageBbox() {
  const crossPageContentBbox = makeMe.pdfLocator.withBbox(
    1,
    [10, 100, 500, 150]
  )
  stubGetBookWithTopMathsLikeContentLocators((i) =>
    i === 0
      ? [makeMe.pdfLocator.pageIndexOnly(0), crossPageContentBbox]
      : [makeMe.bookReading.topMathsLikePreorderFirstLocatorAt(i)]
  )
}

export async function mountFirstBlockBboxScenario(options?: {
  contentFitsInViewport?: boolean
  lastContentBottomVisible?: boolean
}) {
  stubGetBookWithFirstBlockHavingBbox()
  mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  mockIsLastContentBottomVisible(
    wrapper,
    options?.lastContentBottomVisible ?? true
  )
  if (options?.contentFitsInViewport !== undefined) {
    mockSnapBackContentFitsInViewport(wrapper, options.contentFitsInViewport)
  }
  return wrapper
}

export async function mountCrossPageBboxScenario() {
  stubGetBookWithFirstBlockHavingCrossPageBbox()
  mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  mockIsLastContentBottomVisible(wrapper, true)
  return wrapper
}

export async function mountNoDirectContentBboxScenario() {
  const blocks = makeMe.bookReading
    .topMathsLikeFlatBlocks()
    .map((b, i) =>
      i === 0
        ? { ...b, contentLocators: [makeMe.pdfLocator.pageIndexOnly(0)] }
        : b
    )
  vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
    wrapSdkResponse(
      makeMe.aBook
        .id(bookId)
        .notebookId(String(notebookId))
        .blocks(blocks)
        .please()
    )
  )
  mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  return wrapper
}

export async function selectSection1WithVisibleGeometry(
  wrapper: BookReadingPageWrapper
) {
  await clickBookBlockAndExpectSelection(wrapper, "Section 1")
  await emitViewportAndSettleCurrentBlock(wrapper, {
    anchorPageIndexZeroBased: 0,
    viewport: { top: 0, mid: 40, bottom: 600 },
    pagesCount: 10,
  })
}

export async function emitSuccessorCrossing(
  wrapper: BookReadingPageWrapper,
  options?: { mid?: number; top?: number; bottom?: number }
) {
  mockIsLastContentBottomVisible(wrapper, false)
  await emitViewportAndSettleCurrentBlock(wrapper, {
    anchorPageIndexZeroBased: 0,
    viewport: {
      top: options?.top ?? 0,
      mid: options?.mid ?? 200,
      bottom: options?.bottom ?? 600,
    },
    pagesCount: 10,
  })
}

export async function expireSnapHold() {
  await withFakeTimers(async () => {
    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()
  })
}

export async function mountIndependentSnapBudgetsScenario() {
  const section2ContentBbox = makeMe.pdfLocator.withBbox(0, [48, 200, 564, 500])
  stubGetBookWithTopMathsLikeContentLocators((i) =>
    i === 0
      ? [makeMe.pdfLocator.pageIndexOnly(0), firstBlockContentBbox()]
      : i === 1
        ? [
            makeMe.pdfLocator.withBbox(0, [48, 72, 564, 200]),
            section2ContentBbox,
          ]
        : [makeMe.bookReading.topMathsLikePreorderFirstLocatorAt(i)]
  )
  mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  mockIsLastContentBottomVisible(wrapper, true)
  mockSnapBackContentFitsInViewport(wrapper, true)
  return wrapper
}

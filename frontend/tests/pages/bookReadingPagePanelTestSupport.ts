import ReadingControlPanel from "@/components/book-reading/ReadingControlPanel.vue"
import { wrapSdkResponse } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"
import {
  clickBookBlockAndExpectSelection,
  emitViewportAndSettleCurrentBlock,
} from "./bookReadingPageInteractionTestSupport"
import { mockIsLastContentBottomVisible } from "./bookReadingPagePdfViewerTestSupport"
import {
  bookId,
  getTopMathsPdfBytes,
  mockNotebookBookFilePdfOk,
  mountBookReadingPage,
  notebookId,
  stubGetBookWithTopMathsLikeContentLocators,
  waitForPdfViewer,
  type BookReadingPageWrapper,
} from "./bookReadingPageTestSupport"

export async function mountSection2DirectContentBboxBook(options?: {
  mockSuccessorBottomVisible?: boolean
}) {
  const section2ContentBbox = makeMe.pdfLocator.withBbox(0, [48, 72, 564, 200])
  stubGetBookWithTopMathsLikeContentLocators((i) =>
    i === 1
      ? [
          makeMe.bookReading.topMathsLikePreorderFirstLocatorAt(1),
          section2ContentBbox,
        ]
      : [makeMe.bookReading.topMathsLikePreorderFirstLocatorAt(i)]
  )
  mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  if (options?.mockSuccessorBottomVisible) {
    mockIsLastContentBottomVisible(wrapper, true)
  }
  return wrapper
}

async function selectSection1AndShowPanel(wrapper: BookReadingPageWrapper) {
  await clickBookBlockAndExpectSelection(wrapper, "Section 1")
  await emitViewportAndSettleCurrentBlock(wrapper, {
    anchorPageIndexZeroBased: 0,
    viewport: { top: 0, mid: 500, bottom: 1000 },
    pagesCount: 10,
  })
}

export async function markSection1ReadViaPanel(
  wrapper: BookReadingPageWrapper,
  putSpy: ReturnType<typeof vi.spyOn>
) {
  let putCall = 0
  putSpy.mockImplementation(async () => {
    putCall++
    if (putCall === 1) {
      return wrapSdkResponse([])
    }
    return wrapSdkResponse([
      {
        bookBlockId: "101",
        status: "READ" as const,
        completedAt: "2020-01-01T00:00:00Z",
      },
    ])
  })
  await selectSection1AndShowPanel(wrapper)
  await wrapper.findComponent(ReadingControlPanel).vm.$emit("markAsRead")
  await flushPromises()
}

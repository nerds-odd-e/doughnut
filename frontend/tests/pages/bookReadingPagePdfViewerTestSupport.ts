import type { BookReadingPdfViewerRef } from "@/composables/bookReaderViewerRef"
import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import { vi } from "vitest"
import type { BookReadingPageWrapper } from "./bookReadingPageTestSupport"

export function findPdfBookViewer(wrapper: BookReadingPageWrapper) {
  return wrapper.findComponent(PdfBookViewer)
}

function pdfViewerExposed(
  wrapper: BookReadingPageWrapper
): BookReadingPdfViewerRef {
  return (
    findPdfBookViewer(wrapper).vm as unknown as {
      $: { exposed: BookReadingPdfViewerRef }
    }
  ).$.exposed
}

export function pdfScrollRestoreSpy(wrapper: BookReadingPageWrapper) {
  const pdf = findPdfBookViewer(wrapper)
  return {
    pdf,
    spy: vi.spyOn(pdfViewerExposed(wrapper), "scrollToStoredReadingPosition"),
  }
}

export function mockIsLastContentBottomVisible(
  wrapper: BookReadingPageWrapper,
  returnValue: boolean
) {
  vi.spyOn(pdfViewerExposed(wrapper), "isLocatorBottomVisible").mockReturnValue(
    returnValue
  )
}

export function mockReadingPanelAnchorTopPx(
  wrapper: BookReadingPageWrapper,
  returnValue: number | null
) {
  vi.spyOn(
    pdfViewerExposed(wrapper),
    "readingPanelAnchorTopPx"
  ).mockReturnValue(returnValue)
}

/** Scrolling to a target puts the view's top edge at the target's start. */
export function spyOnScrollToBookNavTarget(wrapper: BookReadingPageWrapper) {
  return vi
    .spyOn(pdfViewerExposed(wrapper), "scrollToBookNavigationTarget")
    .mockImplementation(async (target) => {
      stubViewTopAt(wrapper, target.pageIndex, target.bbox?.[1] ?? 0)
    })
}

const PAGE_HEIGHT_PX = 1000

/** Puts the view's top edge at `topY` (0-1000, page-normalized) of `pageIndex`; pages are 1000 px tall. */
export function stubViewTopAt(
  wrapper: BookReadingPageWrapper,
  pageIndex: number,
  topY: number
) {
  vi.spyOn(pdfViewerExposed(wrapper), "viewBlockStarts").mockReturnValue({
    startTopPx: (start) =>
      (start.pageIndex - pageIndex) * PAGE_HEIGHT_PX +
      ((start.bbox?.[1] ?? 0) / 1000) * PAGE_HEIGHT_PX -
      topY,
    landingLimitPx: 0,
  })
}

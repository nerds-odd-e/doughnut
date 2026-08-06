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

export function spyOnScrollPageNormalizedYToReadingClearance(
  wrapper: BookReadingPageWrapper
) {
  return vi.spyOn(
    pdfViewerExposed(wrapper),
    "scrollPageNormalizedYToReadingClearance"
  )
}

/** Matches stub bbox span vs viewport; `fits` mirrors old `contentFitsFromBlockTop` mock. */
export function mockSnapBackContentFitsInViewport(
  wrapper: BookReadingPageWrapper,
  fits: boolean
) {
  const exposed = pdfViewerExposed(wrapper)
  vi.spyOn(exposed, "getPageRect").mockReturnValue({ height: 1000 })
  vi.spyOn(exposed, "getScrollViewportHeightPx").mockReturnValue(
    fits ? 10_000 : 100
  )
}

export function spyOnScrollToBookNavTarget(wrapper: BookReadingPageWrapper) {
  return vi
    .spyOn(pdfViewerExposed(wrapper), "scrollToBookNavigationTarget")
    .mockResolvedValue(undefined)
}

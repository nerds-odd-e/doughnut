import type {
  BookNavigationTarget,
  NormalizedPageBbox,
} from "@/lib/book-reading/pdfOutlineV1Anchor"
import type { ContentLocatorFull } from "@generated/doughnut-backend-api"

export type ViewerLocatorRect = {
  top: number
  bottom: number
  left: number
  right: number
  width: number
  height: number
}

export type BookReaderViewerRef = {
  displayLocator: (locator: ContentLocatorFull) => Promise<void>
  resolveLocatorRect: (locator: ContentLocatorFull) => ViewerLocatorRect | null
  isLocatorBottomVisible: (
    locator: ContentLocatorFull,
    obstructionPx: number
  ) => boolean
  readingPanelAnchorTopPx: (
    locator: ContentLocatorFull,
    obstructionPx: number
  ) => number | null
}

/** Scroll/wheel suppression API registered by `useBookReadingSnapBack` on the PDF viewer. */
export type PdfViewerScrollSuppressionApi = {
  activate: (holdMs: number) => void
  checkEvent: () => boolean
  reset: () => void
  isHoldWindowActive: () => boolean
}

export type BookReadingPdfViewerRef = BookReaderViewerRef & {
  scrollToBookNavigationTarget: (
    target: BookNavigationTarget,
    highlightBboxes?: ReadonlyArray<BookNavigationTarget>
  ) => Promise<void>
  highlightBlockSelection: (
    highlightBboxes: ReadonlyArray<BookNavigationTarget>
  ) => void
  scrollToStoredReadingPosition: (
    pageIndexZeroBased: number,
    normalizedY: number
  ) => Promise<void>
  /** Scroll so `normalizedY` (0–1000) on `pageIndex` sits `obstructionPx` above the container bottom. */
  scrollPageNormalizedYToReadingClearance: (
    pageIndex: number,
    normalizedY: number,
    obstructionPx: number
  ) => void
  afterNextViewUpdate: (fn: () => void) => void
  registerScrollSuppression: (api: PdfViewerScrollSuppressionApi) => () => void
  getPageRect: (pageIndex: number) => { height: number } | null
  getScrollViewportHeightPx: () => number | null
  zoomIn: () => void
  zoomOut: () => void
}

export function locatorAsPdfNavigationTarget(
  locator: ContentLocatorFull
): BookNavigationTarget | null {
  if (locator.type !== "PdfLocator_Full") {
    return null
  }
  const maybe = locator as {
    pageIndex?: unknown
    bbox?: unknown
  }
  if (!Number.isInteger(maybe.pageIndex)) {
    return null
  }
  if (!Array.isArray(maybe.bbox) || maybe.bbox.length !== 4) {
    return null
  }
  const [left, top, right, bottom] = maybe.bbox as unknown[]
  if (![left, top, right, bottom].every((x) => Number.isFinite(x))) {
    return null
  }
  const bbox: NormalizedPageBbox = [
    left as number,
    top as number,
    right as number,
    bottom as number,
  ]
  return {
    pageIndex: maybe.pageIndex as number,
    bbox,
  }
}

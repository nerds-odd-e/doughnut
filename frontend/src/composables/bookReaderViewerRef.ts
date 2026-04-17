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
  snapToContentBottomAndHold: (
    pageIndex: number,
    normalizedBboxBottom: number,
    obstructionPx: number,
    holdMs: number,
    highlightBboxes?: ReadonlyArray<BookNavigationTarget>
  ) => void
  suppressScrollInput: (holdMs: number) => void
  contentFitsFromBlockTop: (
    pageIndex: number,
    normalizedBlockTopY: number,
    normalizedContentBottomY: number,
    obstructionPx: number
  ) => boolean
  zoomIn: () => void
  zoomOut: () => void
}

export function locatorAsPdfNavigationTarget(
  locator: ContentLocatorFull
): BookNavigationTarget | null {
  const tag = locator.type as string
  if (tag !== "PdfLocator_Full" && tag !== "pdf") {
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

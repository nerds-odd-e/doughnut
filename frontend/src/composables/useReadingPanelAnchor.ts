import type { BookReaderViewerRef } from "@/composables/bookReaderViewerRef"
import { lastDirectContentLocator } from "@/lib/book-reading/bookBlockDirectContent"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import { ref, type Ref } from "vue"

export const READING_PANEL_OBSTRUCTION_PX = 80
export const MIN_READING_PANEL_RESERVE_PX = 88

const MAIN_PANE_ANCHOR_EDGE_INSET_PX = 8

type MinimalViewerRef = Pick<BookReaderViewerRef, "readingPanelAnchorTopPx">

export function clampReadingPanelAnchorTop(
  top: number | null,
  mainPaneHeightPx: number,
  minReservePx: number = MIN_READING_PANEL_RESERVE_PX,
  edgeInsetPx: number = MAIN_PANE_ANCHOR_EDGE_INSET_PX
): number | null {
  if (top === null) return null
  if (
    mainPaneHeightPx > 0 &&
    top + minReservePx > mainPaneHeightPx - edgeInsetPx
  ) {
    return null
  }
  return top
}

export function useReadingPanelAnchor(options: {
  viewerRef: Ref<MinimalViewerRef | null>
  blockRef: Ref<BookBlockFull | null>
  mainPaneRef: Ref<HTMLElement | null>
  obstructionPx?: number
  minReservePx?: number
}) {
  const obstructionPx = options.obstructionPx ?? READING_PANEL_OBSTRUCTION_PX
  const minReservePx = options.minReservePx ?? MIN_READING_PANEL_RESERVE_PX

  const readingPanelAnchorTopPx = ref<number | null>(null)

  function updateReadingPanelAnchor() {
    const mainEl = options.mainPaneRef.value
    const viewer = options.viewerRef.value
    const block = options.blockRef.value
    if (!mainEl || !viewer || !block) {
      readingPanelAnchorTopPx.value = null
      return
    }
    const lastLocator = lastDirectContentLocator(block)
    if (lastLocator === null) {
      readingPanelAnchorTopPx.value = null
      return
    }
    let top = viewer.readingPanelAnchorTopPx(lastLocator, obstructionPx)
    if (top !== null) {
      const mainH = mainEl.getBoundingClientRect().height
      top = clampReadingPanelAnchorTop(top, mainH, minReservePx)
    }
    readingPanelAnchorTopPx.value = top
  }

  return { readingPanelAnchorTopPx, updateReadingPanelAnchor }
}

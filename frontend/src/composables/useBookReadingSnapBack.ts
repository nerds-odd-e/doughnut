import {
  asPdfLocator,
  pdfLocatorsFromBlock,
} from "@/lib/book-reading/asPdfLocator"
import { lastDirectContentLocator } from "@/lib/book-reading/bookBlockDirectContent"
import { createIntervalScrollSuppression } from "@/lib/book-reading/intervalScrollSuppression"
import { wireItemsToNavigationTargets } from "@/lib/book-reading/pdfOutlineV1Anchor"
import type { BookReadingPdfViewerRef } from "@/composables/bookReaderViewerRef"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import {
  computed,
  type ComputedRef,
  onScopeDispose,
  type Ref,
  ref,
  watch,
} from "vue"

/** Whether the normalized vertical span on the page fits in the viewport minus obstruction. */
export function snapBackNormalizedSpanFitsViewport(options: {
  pageHeightPx: number
  viewportHeightPx: number
  normalizedBlockTopY: number
  normalizedContentBottomY: number
  obstructionPx: number
}): boolean {
  const spanPx =
    ((options.normalizedContentBottomY - options.normalizedBlockTopY) / 1000) *
    options.pageHeightPx
  return spanPx <= options.viewportHeightPx - options.obstructionPx
}

function contentFitsFromBlockTop(options: {
  viewer: BookReadingPdfViewerRef | null
  pageIndex: number
  normalizedBlockTopY: number
  normalizedContentBottomY: number
  obstructionPx: number
}): boolean {
  const {
    viewer,
    pageIndex,
    normalizedBlockTopY,
    normalizedContentBottomY,
    obstructionPx,
  } = options
  if (!viewer) return false
  const pageRect = viewer.getPageRect(pageIndex)
  const viewportH = viewer.getScrollViewportHeightPx()
  if (pageRect === null || viewportH === null) return false
  return snapBackNormalizedSpanFitsViewport({
    pageHeightPx: pageRect.height,
    viewportHeightPx: viewportH,
    normalizedBlockTopY,
    normalizedContentBottomY,
    obstructionPx,
  })
}

function selectedIndexAndSuccessor(
  rows: readonly BookBlockFull[],
  selId: number
): {
  selIdx: number
  sel: BookBlockFull
  successor: BookBlockFull
} | null {
  const selIdx = rows.findIndex((r) => r.id === selId)
  if (selIdx < 0 || selIdx >= rows.length - 1) {
    return null
  }
  return {
    selIdx,
    sel: rows[selIdx]!,
    successor: rows[selIdx + 1]!,
  }
}

function hasDirectContent(row: BookBlockFull): boolean {
  return row.contentLocators.length > 1
}

export function useBookReadingSnapBack(options: {
  bookBlocks: ComputedRef<readonly BookBlockFull[]>
  selectedBlockId: Ref<number | null>
  currentBlockId: Readonly<Ref<number | null>>
  hasRecordedDisposition: (id: number) => boolean
  pdfViewerRef: Ref<BookReadingPdfViewerRef | null>
  obstructionPx: number
  snapHoldMs: number
}) {
  const {
    bookBlocks,
    selectedBlockId,
    currentBlockId,
    hasRecordedDisposition,
    pdfViewerRef,
    obstructionPx,
    snapHoldMs,
  } = options

  const lastContentBottomVisible = ref(false)
  const geometryEverVisibleForSelection = ref(false)
  const snapbackAttempts = new Map<number, number>()
  const snapAnimationKey = ref(0)
  const scrollSuppression = createIntervalScrollSuppression()
  let unregisterScrollSuppression: (() => void) | null = null

  watch(
    pdfViewerRef,
    (v) => {
      unregisterScrollSuppression?.()
      unregisterScrollSuppression =
        v?.registerScrollSuppression?.(scrollSuppression) ?? null
    },
    { immediate: true }
  )

  onScopeDispose(() => {
    unregisterScrollSuppression?.()
    unregisterScrollSuppression = null
    scrollSuppression.reset()
  })

  /**
   * Trigger A: the user has seen the selected block's content geometry at
   * some point and has since scrolled past the immediate successor — a
   * "reminder" trigger that fires independently of snap-back.
   */
  function panelShownBecauseScrolledPastContent(
    successor: BookBlockFull
  ): boolean {
    return (
      geometryEverVisibleForSelection.value &&
      successor.id !== currentBlockId.value
    )
  }

  function shouldSnapBack(proposedBlockId: number | null): boolean {
    if (proposedBlockId === null) return false
    const selId = selectedBlockId.value
    if (selId === null) return false
    if (hasRecordedDisposition(selId)) return false
    const rows = bookBlocks.value
    const chain = selectedIndexAndSuccessor(rows, selId)
    if (chain === null) return false
    const { selIdx, sel, successor } = chain
    const proposedIdx = rows.findIndex((r) => r.id === proposedBlockId)
    if (proposedIdx < 0 || proposedIdx <= selIdx) return false
    const immediateSuccessor = proposedBlockId === successor.id
    if (!immediateSuccessor) {
      if (proposedIdx <= selIdx + 1) return false
      for (let i = selIdx + 1; i < proposedIdx; i++) {
        if (!hasDirectContent(rows[i]!)) return false
      }
    }
    if (!geometryEverVisibleForSelection.value) return false
    if (!hasDirectContent(sel)) return false
    return (snapbackAttempts.get(selId) ?? 0) < 2
  }

  function performSnapBack(): void {
    const selId = selectedBlockId.value
    if (selId === null) return
    snapAnimationKey.value += 1
    const rows = bookBlocks.value
    const sel = rows.find((r) => r.id === selId)
    if (!sel) return
    const lastLoc = lastDirectContentLocator(sel)
    if (lastLoc === null) return
    const lastBbox = asPdfLocator(lastLoc)
    if (lastBbox === null) return
    snapbackAttempts.set(selId, (snapbackAttempts.get(selId) ?? 0) + 1)
    const navTargets = wireItemsToNavigationTargets(pdfLocatorsFromBlock(sel))
    const parsedStart = navTargets[0] ?? null
    const contentBottomY = (lastBbox.bbox as number[])[3]!
    const samePage =
      parsedStart !== null && parsedStart.pageIndex === lastBbox.pageIndex
    const blockTopY =
      samePage && parsedStart!.bbox !== null ? parsedStart!.bbox[1] : 0
    const fits =
      samePage &&
      contentFitsFromBlockTop({
        viewer: pdfViewerRef.value,
        pageIndex: lastBbox.pageIndex,
        normalizedBlockTopY: blockTopY,
        normalizedContentBottomY: contentBottomY,
        obstructionPx,
      })
    const viewer = pdfViewerRef.value
    if (viewer === null) return
    if (fits) {
      viewer
        .scrollToBookNavigationTarget(parsedStart!, navTargets)
        .then(() => {
          scrollSuppression.activate(snapHoldMs)
        })
        .catch(() => undefined)
    } else {
      scrollSuppression.activate(snapHoldMs)
      viewer.scrollPageNormalizedYToReadingClearance(
        lastBbox.pageIndex,
        contentBottomY,
        obstructionPx
      )
      viewer.afterNextViewUpdate(() => {
        pdfViewerRef.value?.highlightBlockSelection(navTargets)
      })
    }
  }

  const confirmationTargetBlock = computed<BookBlockFull | null>(() => {
    const selId = selectedBlockId.value
    if (selId === null) return null
    const rows = bookBlocks.value
    if (hasRecordedDisposition(selId)) {
      const selIdx = rows.findIndex((r) => r.id === selId)
      if (selIdx < 0 || selIdx >= rows.length - 1) return null
      const successor = rows[selIdx + 1]!
      if (hasRecordedDisposition(successor.id) || !hasDirectContent(successor))
        return null
      return successor
    }
    return rows.find((r) => r.id === selId) ?? null
  })

  const blockAwaitingConfirmation = computed<BookBlockFull | null>(() => {
    const target = confirmationTargetBlock.value
    if (target === null) return null
    const selId = selectedBlockId.value!
    if (hasRecordedDisposition(selId)) {
      return lastContentBottomVisible.value ? target : null
    }
    const rows = bookBlocks.value
    const chain = selectedIndexAndSuccessor(rows, selId)
    if (chain === null) {
      return hasDirectContent(target) && lastContentBottomVisible.value
        ? target
        : null
    }
    const { successor } = chain
    if (lastDirectContentLocator(target) !== null) {
      const contentBottomVisible = lastContentBottomVisible.value
      const scrolledPastContent =
        panelShownBecauseScrolledPastContent(successor)
      return contentBottomVisible || scrolledPastContent ? target : null
    }
    return successor.id === currentBlockId.value ? target : null
  })

  function updateLastDirectContentGeometry(): void {
    const target = confirmationTargetBlock.value
    const lastLocatorForGeometry =
      target !== null ? lastDirectContentLocator(target) : null
    if (lastLocatorForGeometry === null) return
    const geometryVisible =
      pdfViewerRef.value?.isLocatorBottomVisible(
        lastLocatorForGeometry,
        obstructionPx
      ) ?? false
    lastContentBottomVisible.value = geometryVisible
    if (geometryVisible) {
      geometryEverVisibleForSelection.value = true
    }
  }

  function clearSnapbackAttemptsForBlock(blockId: number): void {
    snapbackAttempts.delete(blockId)
  }

  watch(selectedBlockId, () => {
    lastContentBottomVisible.value = false
    geometryEverVisibleForSelection.value = false
    snapbackAttempts.clear()
  })

  return {
    snapAnimationKey,
    blockAwaitingConfirmation,
    lastContentBottomVisible,
    shouldSnapBack,
    performSnapBack,
    updateLastDirectContentGeometry,
    clearSnapbackAttemptsForBlock,
  }
}

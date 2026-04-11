import { wireItemsToNavigationTargets } from "@/lib/book-reading/pdfOutlineV1Anchor"
import type { BookNavigationTarget } from "@/lib/book-reading/pdfOutlineV1Anchor"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import { computed, type ComputedRef, type Ref, ref, watch } from "vue"

export type BookReadingPdfViewerRef = {
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
    holdMs: number
  ) => void
  suppressScrollInput: (holdMs: number) => void
  zoomIn: () => void
  zoomOut: () => void
  isLastContentBottomVisible: (
    target: BookNavigationTarget,
    obstructionPx: number
  ) => boolean
  readingPanelAnchorTopPx: (
    mainEl: HTMLElement,
    target: BookNavigationTarget,
    obstructionPx: number
  ) => number | null
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
  return row.allBboxes.length > 1
}

function lastContentBbox(row: BookBlockFull) {
  return hasDirectContent(row) ? row.allBboxes[row.allBboxes.length - 1]! : null
}

export function useBookReadingSnapBack(options: {
  bookBlocks: ComputedRef<readonly BookBlockFull[]>
  selectedBlockId: Ref<number | null>
  currentBlockId: Ref<number | null>
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
    const lastBbox = lastContentBbox(sel)
    if (lastBbox === null) return
    snapbackAttempts.set(selId, (snapbackAttempts.get(selId) ?? 0) + 1)
    const navTargets = wireItemsToNavigationTargets(sel.allBboxes)
    const parsedStart = navTargets[0] ?? null
    if (parsedStart !== null && parsedStart.pageIndex === lastBbox.pageIndex) {
      pdfViewerRef.value
        ?.scrollToBookNavigationTarget(parsedStart, navTargets)
        .then(() => pdfViewerRef.value?.suppressScrollInput(snapHoldMs))
        .catch(() => undefined)
    } else {
      pdfViewerRef.value?.snapToContentBottomAndHold(
        lastBbox.pageIndex,
        (lastBbox.bbox as number[])[3]!,
        obstructionPx,
        snapHoldMs
      )
    }
  }

  const blockAwaitingConfirmation = computed<BookBlockFull | null>(() => {
    const selId = selectedBlockId.value
    if (selId === null) return null
    if (hasRecordedDisposition(selId)) return null
    const rows = bookBlocks.value
    const chain = selectedIndexAndSuccessor(rows, selId)
    if (chain === null) return null
    const { sel, successor } = chain
    const lastBbox = lastContentBbox(sel)
    if (lastBbox !== null) {
      if (lastContentBottomVisible.value) return sel
      if (geometryEverVisibleForSelection.value) {
        return successor.id === currentBlockId.value ? null : sel
      }
      return null
    }
    return successor.id === currentBlockId.value ? sel : null
  })

  function updateLastDirectContentGeometry(): void {
    const selIdForGeometry = selectedBlockId.value
    if (selIdForGeometry === null) return
    const selForGeometry = bookBlocks.value.find(
      (r) => r.id === selIdForGeometry
    )
    const lastBboxForGeometry =
      selForGeometry !== undefined ? lastContentBbox(selForGeometry) : null
    if (lastBboxForGeometry === null) return
    const geometryVisible =
      pdfViewerRef.value?.isLastContentBottomVisible(
        {
          pageIndex: lastBboxForGeometry.pageIndex,
          bbox: lastBboxForGeometry.bbox as [number, number, number, number],
        },
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
    hasDirectContent,
  }
}

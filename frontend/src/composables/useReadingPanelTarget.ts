import { lastDirectContentLocator } from "@/lib/book-reading/bookBlockDirectContent"
import type { BookReadingPdfViewerRef } from "@/composables/bookReaderViewerRef"
import type { BookBlockFull } from "@generated/donut-backend-api"
import { computed, type ComputedRef, type Ref, ref, watch } from "vue"

function successorOf(
  rows: readonly BookBlockFull[],
  selId: number
): BookBlockFull | null {
  const selIdx = rows.findIndex((r) => r.id === selId)
  if (selIdx < 0 || selIdx >= rows.length - 1) {
    return null
  }
  return rows[selIdx + 1]!
}

function hasDirectContent(row: BookBlockFull): boolean {
  return lastDirectContentLocator(row) !== null
}

export function useReadingPanelTarget(options: {
  bookBlocks: ComputedRef<readonly BookBlockFull[]>
  selectedBlockId: Ref<number | null>
  currentBlockId: Readonly<Ref<number | null>>
  hasRecordedDisposition: (id: number) => boolean
  pdfViewerRef: Ref<BookReadingPdfViewerRef | null>
  obstructionPx: number
}) {
  const {
    bookBlocks,
    selectedBlockId,
    currentBlockId,
    hasRecordedDisposition,
    pdfViewerRef,
    obstructionPx,
  } = options

  const lastContentBottomVisible = ref(false)
  const geometryEverVisibleForSelection = ref(false)

  const confirmationTargetBlock = computed<BookBlockFull | null>(() => {
    const selId = selectedBlockId.value
    if (selId === null) return null
    if (!hasRecordedDisposition(selId)) {
      return bookBlocks.value.find((r) => r.id === selId) ?? null
    }
    const successor = successorOf(bookBlocks.value, selId)
    if (
      successor === null ||
      hasRecordedDisposition(successor.id) ||
      !hasDirectContent(successor)
    )
      return null
    return successor
  })

  const blockAwaitingConfirmation = computed<BookBlockFull | null>(() => {
    const target = confirmationTargetBlock.value
    if (target === null) return null
    const selId = selectedBlockId.value!
    if (hasRecordedDisposition(selId)) {
      return lastContentBottomVisible.value ? target : null
    }
    const successor = successorOf(bookBlocks.value, selId)
    if (successor === null) {
      return hasDirectContent(target) && lastContentBottomVisible.value
        ? target
        : null
    }
    if (hasDirectContent(target)) {
      return geometryEverVisibleForSelection.value ? target : null
    }
    return successor.id === currentBlockId.value ? target : null
  })

  /** The offered block while its content end is on screen, for anchoring the panel beneath it. */
  const anchoredBlock = computed(() =>
    lastContentBottomVisible.value ? blockAwaitingConfirmation.value : null
  )

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

  watch(selectedBlockId, () => {
    lastContentBottomVisible.value = false
    geometryEverVisibleForSelection.value = false
  })

  return {
    blockAwaitingConfirmation,
    anchoredBlock,
    updateLastDirectContentGeometry,
  }
}

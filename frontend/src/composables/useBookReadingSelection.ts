import { useAutoMarkNoDirectContentPredecessor } from "@/composables/useAutoMarkNoDirectContentPredecessor"
import { nextBookBlockAfter } from "@/lib/book-reading/nextBookBlockAfter"
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import {
  computed,
  ref,
  toValue,
  watch,
  type ComputedRef,
  type MaybeRefOrGetter,
  type Ref,
} from "vue"

export function useBookReadingSelection(options: {
  bookBlocks: MaybeRefOrGetter<readonly BookBlockFull[]>
  currentBlockId: Ref<number | null>
  hasRecordedDisposition: (id: number) => boolean
  submitReadingDisposition: (
    bookBlockId: number,
    status: BookBlockReadingDisposition
  ) => Promise<boolean>
  onAdvance: (block: BookBlockFull) => void | Promise<void>
  /** Called after `onAdvance` from `applyBookBlockSelection` (e.g. EPUB anchor refresh). */
  afterAdvance?: () => void | Promise<void>
  initialSelectedBlockId?: number | null
  /** When set (PDF), snap-back supplies the panel target; otherwise EPUB-style. */
  overrideBlockAwaitingConfirmation?: ComputedRef<BookBlockFull | null>
  /**
   * PDF: keep selection valid when `book.blocks` changes (first block fallback).
   * EPUB: leave false to preserve prior behavior.
   */
  repairSelectionWhenBlocksChange?: boolean
  onMarkedRead?: (blockId: number) => void
  selectedBlockId?: Ref<number | null>
}): {
  selectedBlockId: Ref<number | null>
  blockAwaitingConfirmation: ComputedRef<BookBlockFull | null>
  applyBookBlockSelection: (block: BookBlockFull) => Promise<void>
  markSelectedBlockDisposition: (
    status: BookBlockReadingDisposition
  ) => Promise<void>
} {
  const {
    bookBlocks,
    currentBlockId,
    hasRecordedDisposition,
    submitReadingDisposition,
    onAdvance,
    afterAdvance,
    initialSelectedBlockId = null,
    overrideBlockAwaitingConfirmation,
    repairSelectionWhenBlocksChange = false,
    onMarkedRead,
    selectedBlockId: selectedBlockIdOption,
  } = options

  const selectedBlockId =
    selectedBlockIdOption ?? ref<number | null>(initialSelectedBlockId)

  const defaultBlockAwaitingConfirmation = computed<BookBlockFull | null>(
    () => {
      const selId = selectedBlockId.value
      if (selId === null) return null
      if (hasRecordedDisposition(selId)) return null
      const rows = toValue(bookBlocks)
      return rows.find((b) => b.id === selId) ?? null
    }
  )

  const blockAwaitingConfirmation =
    overrideBlockAwaitingConfirmation ?? defaultBlockAwaitingConfirmation

  useAutoMarkNoDirectContentPredecessor({
    bookBlocks,
    currentBlockId,
    hasRecordedDisposition,
    submitReadingDisposition,
  })

  if (repairSelectionWhenBlocksChange) {
    watch(
      () => toValue(bookBlocks),
      (blocks) => {
        if (blocks.length === 0) {
          selectedBlockId.value = null
          return
        }
        const id = selectedBlockId.value
        if (id === null || !blocks.some((r) => r.id === id)) {
          selectedBlockId.value = blocks[0]!.id
        }
      },
      { immediate: true }
    )
  }

  async function applyBookBlockSelection(block: BookBlockFull) {
    await onAdvance(block)
    await afterAdvance?.()
  }

  async function markSelectedBlockDisposition(
    status: BookBlockReadingDisposition
  ) {
    const block = blockAwaitingConfirmation.value
    if (!block) {
      return
    }
    const ok = await submitReadingDisposition(block.id, status)
    if (!ok) {
      return
    }
    if (status === "READ") {
      onMarkedRead?.(block.id)
    }
    const next = nextBookBlockAfter(toValue(bookBlocks), block.id)
    if (next) {
      await applyBookBlockSelection(next)
    }
  }

  return {
    selectedBlockId,
    blockAwaitingConfirmation,
    applyBookBlockSelection,
    markSelectedBlockDisposition,
  }
}

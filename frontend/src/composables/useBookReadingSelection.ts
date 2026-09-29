import { useAutoMarkPredecessorWithNoTextOfItsOwn } from "@/composables/useAutoMarkPredecessorWithNoTextOfItsOwn"
import { nextBookBlockAfter } from "@/lib/book-reading/nextBookBlockAfter"
import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type { BookBlockFull } from "@generated/donut-backend-api"
import {
  computed,
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
  /** Decides which block awaits confirmation instead of the unmarked selected block. */
  blockAwaitingConfirmation?: () => BookBlockFull | null
  /** Keeps the selection on an existing block (the first) when the blocks change. */
  repairSelection?: boolean
  onMarkedRead?: (blockId: number) => void
  selectedBlockId: Ref<number | null>
}): {
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
    repairSelection = false,
    onMarkedRead,
    selectedBlockId,
  } = options

  const blockAwaitingConfirmation = computed<BookBlockFull | null>(
    options.blockAwaitingConfirmation ??
      (() => {
        const selId = selectedBlockId.value
        if (selId === null) return null
        if (hasRecordedDisposition(selId)) return null
        return toValue(bookBlocks).find((b) => b.id === selId) ?? null
      })
  )

  useAutoMarkPredecessorWithNoTextOfItsOwn({
    bookBlocks,
    currentBlockId,
    hasRecordedDisposition,
    submitReadingDisposition,
  })

  if (repairSelection) {
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
    blockAwaitingConfirmation,
    applyBookBlockSelection,
    markSelectedBlockDisposition,
  }
}

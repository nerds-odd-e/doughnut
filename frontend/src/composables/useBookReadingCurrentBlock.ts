import {
  createCurrentBlockIdDebouncer,
  type CurrentBlockIdDebouncer,
} from "@/lib/book-reading/debounceCurrentBlockId"
import {
  createLastReadPositionPatchDebouncer,
  type LastReadPositionPatchDebouncer,
} from "@/lib/book-reading/debounceLastReadPositionPatch"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import {
  onBeforeUnmount,
  toValue,
  watch,
  type DeepReadonly,
  type MaybeRefOrGetter,
  type Ref,
} from "vue"

const DEFAULT_CURRENT_BLOCK_DEBOUNCE_MS = 120
const DEFAULT_LAST_READ_PATCH_DEBOUNCE_MS = 400

export function useBookReadingCurrentBlock(options: {
  notebookId: MaybeRefOrGetter<number>
  commitCurrentBlock: (id: number | null) => boolean
  /**
   * Format-specific: receives the PATCH debouncer (single `propose(locator, selectedBookBlockId?)`),
   * returns the function to run when the reading position should be sent (viewport/relocate updates
   * and when `currentBlockId` changes).
   */
  proposeReadingPosition: (
    debouncer: LastReadPositionPatchDebouncer
  ) => () => void
  currentBlockDebounceMs?: number
  lastReadPositionPatchDebounceMs?: number
  /** EPUB flushes a pending PATCH on leave; PDF cancels without sending. */
  flushLastReadPositionPatchOnUnmount?: boolean
}): {
  currentBlockId: DeepReadonly<Ref<number | null>>
  currentBlockIdDebouncer: CurrentBlockIdDebouncer
  lastReadPositionPatchDebouncer: LastReadPositionPatchDebouncer
  proposeReadingPosition: () => void
} {
  const currentBlockDebounceMs =
    options.currentBlockDebounceMs ?? DEFAULT_CURRENT_BLOCK_DEBOUNCE_MS
  const lastReadPatchMs =
    options.lastReadPositionPatchDebounceMs ??
    DEFAULT_LAST_READ_PATCH_DEBOUNCE_MS

  const lastReadPositionPatchDebouncer = createLastReadPositionPatchDebouncer({
    delayMs: lastReadPatchMs,
    patch: (body) =>
      NotebookBooksController.patchNotebookBookReadingPosition({
        path: { notebook: toValue(options.notebookId) },
        body,
      }),
  })

  const proposeReadingPosition = options.proposeReadingPosition(
    lastReadPositionPatchDebouncer
  )

  const currentBlockIdDebouncer = createCurrentBlockIdDebouncer({
    delayMs: currentBlockDebounceMs,
    commit: options.commitCurrentBlock,
  })

  const { currentBlockId } = currentBlockIdDebouncer

  watch(currentBlockId, () => {
    proposeReadingPosition()
  })

  onBeforeUnmount(() => {
    currentBlockIdDebouncer.cancel()
    if (options.flushLastReadPositionPatchOnUnmount) {
      lastReadPositionPatchDebouncer.flush()
    } else {
      lastReadPositionPatchDebouncer.cancel()
    }
  })

  return {
    currentBlockId,
    currentBlockIdDebouncer,
    lastReadPositionPatchDebouncer,
    proposeReadingPosition,
  }
}

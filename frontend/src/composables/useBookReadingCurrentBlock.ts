import {
  createCurrentBlockIdDebouncer,
  type CurrentBlockIdDebouncer,
} from "@/lib/book-reading/debounceCurrentBlockId"
import {
  createLastReadPositionPatchDebouncer,
  type LastReadPositionPatchBody,
} from "@/lib/book-reading/debounceLastReadPositionPatch"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import {
  onBeforeUnmount,
  toValue,
  watch,
  type DeepReadonly,
  type MaybeRefOrGetter,
  type Ref,
} from "vue"

const CURRENT_BLOCK_DEBOUNCE_MS = 120
const LAST_READ_PATCH_DEBOUNCE_MS = 400

export function useBookReadingCurrentBlock(options: {
  notebookId: MaybeRefOrGetter<number>
  /** The reading position to send, or null when there is none yet. */
  readingPosition: () => LastReadPositionPatchBody | null
  /** EPUB sends a pending reading position on leave; PDF drops it. */
  flushPositionOnLeave?: boolean
}): {
  currentBlockId: DeepReadonly<Ref<number | null>>
  currentBlockIdDebouncer: CurrentBlockIdDebouncer
  proposeReadingPosition: () => void
} {
  const lastReadPositionPatchDebouncer = createLastReadPositionPatchDebouncer({
    delayMs: LAST_READ_PATCH_DEBOUNCE_MS,
    patch: (body) =>
      NotebookBooksController.patchNotebookBookReadingPosition({
        path: { notebook: toValue(options.notebookId) },
        body,
      }),
  })

  function proposeReadingPosition() {
    const position = options.readingPosition()
    if (position === null) return
    lastReadPositionPatchDebouncer.propose(
      position.locator,
      position.selectedBookBlockId
    )
  }

  const currentBlockIdDebouncer = createCurrentBlockIdDebouncer({
    delayMs: CURRENT_BLOCK_DEBOUNCE_MS,
  })

  const { currentBlockId } = currentBlockIdDebouncer

  watch(currentBlockId, () => {
    proposeReadingPosition()
  })

  onBeforeUnmount(() => {
    currentBlockIdDebouncer.cancel()
    if (options.flushPositionOnLeave) {
      lastReadPositionPatchDebouncer.flush()
    } else {
      lastReadPositionPatchDebouncer.cancel()
    }
  })

  return {
    currentBlockId,
    currentBlockIdDebouncer,
    proposeReadingPosition,
  }
}

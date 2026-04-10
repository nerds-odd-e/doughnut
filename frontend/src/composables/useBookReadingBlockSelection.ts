import { onWatcherCleanup, watch, type Ref } from "vue"
import type { BookAnchorFull } from "@generated/doughnut-backend-api"

export const AUTO_SELECT_BOOK_BLOCK_DWELL_MS = 5000

export type BookBlockRowForSelection = {
  id: number
  startAnchor: BookAnchorFull
}

/**
 * When the viewport-derived current block anchor stays stable for {@link AUTO_SELECT_BOOK_BLOCK_DWELL_MS},
 * calls `onDwellSelectBlock` with the matching layout row. Selection + PDF highlight should be applied
 * in that callback (same path as layout click).
 */
export function useBookReadingBlockSelection(options: {
  bookBlockRows: () => ReadonlyArray<BookBlockRowForSelection>
  currentBlockAnchorId: Ref<number | null>
  onDwellSelectBlock: (row: BookBlockRowForSelection) => void | Promise<void>
}) {
  watch(
    () => options.currentBlockAnchorId.value,
    (anchorId) => {
      let timer: number | undefined

      onWatcherCleanup(() => {
        if (timer !== undefined) {
          window.clearTimeout(timer)
          timer = undefined
        }
      })

      if (anchorId === null) {
        return
      }

      const scheduledFor = anchorId
      timer = window.setTimeout(() => {
        timer = undefined
        const still = options.currentBlockAnchorId.value
        if (still !== scheduledFor) {
          return
        }
        const row = options
          .bookBlockRows()
          .find((r) => r.startAnchor.id === still)
        if (!row) {
          return
        }
        Promise.resolve(options.onDwellSelectBlock(row)).catch(() => undefined)
      }, AUTO_SELECT_BOOK_BLOCK_DWELL_MS)
    }
  )
}

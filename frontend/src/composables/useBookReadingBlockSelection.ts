import { onWatcherCleanup, watch, type Ref } from "vue"
import type { PageBboxFull } from "@generated/doughnut-backend-api"

export const AUTO_SELECT_BOOK_BLOCK_DWELL_MS = 5000

export type BookBlockRowForSelection = {
  id: number
  allBboxes: PageBboxFull[]
}

/**
 * When the viewport-derived current block id stays stable for {@link AUTO_SELECT_BOOK_BLOCK_DWELL_MS},
 * calls `onDwellSelectBlock` with the matching layout row. Selection + PDF highlight should be applied
 * in that callback (same path as layout click).
 */
export function useBookReadingBlockSelection(options: {
  bookBlockRows: () => ReadonlyArray<BookBlockRowForSelection>
  currentBlockId: Ref<number | null>
  onDwellSelectBlock: (row: BookBlockRowForSelection) => void | Promise<void>
}) {
  watch(
    () => options.currentBlockId.value,
    (blockId) => {
      let timer: number | undefined

      onWatcherCleanup(() => {
        if (timer !== undefined) {
          window.clearTimeout(timer)
          timer = undefined
        }
      })

      if (blockId === null) {
        return
      }

      const scheduledFor = blockId
      timer = window.setTimeout(() => {
        timer = undefined
        const still = options.currentBlockId.value
        if (still !== scheduledFor) {
          return
        }
        const row = options.bookBlockRows().find((r) => r.id === still)
        if (!row) {
          return
        }
        Promise.resolve(options.onDwellSelectBlock(row)).catch(() => undefined)
      }, AUTO_SELECT_BOOK_BLOCK_DWELL_MS)
    }
  )
}

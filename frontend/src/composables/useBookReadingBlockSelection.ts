import { onWatcherCleanup, watch, type Ref } from "vue"
import type { BookBlockFull } from "@generated/doughnut-backend-api"

export const AUTO_SELECT_BOOK_BLOCK_DWELL_MS = 5000

/**
 * When the viewport-derived current block id stays stable for {@link AUTO_SELECT_BOOK_BLOCK_DWELL_MS},
 * calls `onDwellSelectBlock` with the matching block. Selection + PDF highlight should be applied
 * in that callback (same path as layout click).
 */
export function useBookReadingBlockSelection(options: {
  blocks: () => ReadonlyArray<BookBlockFull>
  currentBlockId: Ref<number | null>
  onDwellSelectBlock: (block: BookBlockFull) => void | Promise<void>
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
        const block = options.blocks().find((b) => b.id === still)
        if (!block) {
          return
        }
        Promise.resolve(options.onDwellSelectBlock(block)).catch(
          () => undefined
        )
      }, AUTO_SELECT_BOOK_BLOCK_DWELL_MS)
    }
  )
}

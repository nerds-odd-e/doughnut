import type { BookBlockReadingDisposition } from "@/lib/book-reading/readBlockIdsFromRecords"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import { toValue, unref, type MaybeRefOrGetter, type Ref, watch } from "vue"

export function useAutoMarkNoDirectContentPredecessor(options: {
  bookBlocks: MaybeRefOrGetter<readonly BookBlockFull[]>
  currentBlockId: Readonly<Ref<number | null>>
  hasRecordedDisposition: (id: number) => boolean
  submitReadingDisposition: (
    bookBlockId: number,
    status: BookBlockReadingDisposition
  ) => Promise<boolean>
}): void {
  const {
    bookBlocks,
    currentBlockId,
    hasRecordedDisposition,
    submitReadingDisposition,
  } = options

  watch(
    () => unref(currentBlockId),
    async (blockId) => {
      if (blockId === null) return
      const rows = toValue(bookBlocks)
      const bIdx = rows.findIndex((r) => r.id === blockId)
      if (bIdx <= 0) return
      const predecessor = rows[bIdx - 1]!
      const locs = predecessor.contentLocators
      if (
        locs.length > 0 &&
        locs.length <= 1 &&
        !hasRecordedDisposition(predecessor.id)
      ) {
        await submitReadingDisposition(predecessor.id, "READ")
      }
    }
  )
}

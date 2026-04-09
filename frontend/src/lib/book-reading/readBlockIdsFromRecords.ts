import type { BookBlockReadingRecordListItem } from "@generated/doughnut-backend-api"

export const BOOK_BLOCK_READING_STATUS_READ = "READ"

export function readBlockIdsFromRecords(
  items: BookBlockReadingRecordListItem[]
): Set<number> {
  const ids = new Set<number>()
  for (const r of items) {
    if (r.status === BOOK_BLOCK_READING_STATUS_READ) {
      ids.add(Number(r.bookBlockId))
    }
  }
  return ids
}

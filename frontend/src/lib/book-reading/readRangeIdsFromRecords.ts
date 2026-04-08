import type { BookRangeReadingRecordListItem } from "@generated/doughnut-backend-api"

export const BOOK_RANGE_READING_STATUS_READ = "READ"

export function readRangeIdsFromRecords(
  items: BookRangeReadingRecordListItem[]
): Set<number> {
  const ids = new Set<number>()
  for (const r of items) {
    if (r.status === BOOK_RANGE_READING_STATUS_READ) {
      ids.add(Number(r.bookRangeId))
    }
  }
  return ids
}

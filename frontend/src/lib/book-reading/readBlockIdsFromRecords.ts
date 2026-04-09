import type {
  BookBlockReadingRecordListItem,
  BookBlockReadingRecordPutRequest,
} from "@generated/doughnut-backend-api"

/** Same values as `BookBlockReadingRecordPutRequest.status` on the wire. */
export type BookBlockReadingDisposition =
  BookBlockReadingRecordPutRequest["status"]

const ALL_DISPOSITIONS = [
  "READ",
  "SKIMMED",
  "SKIPPED",
] as const satisfies readonly BookBlockReadingDisposition[]

function isDisposition(value: string): value is BookBlockReadingDisposition {
  return (ALL_DISPOSITIONS as readonly string[]).includes(value)
}

export function readingDispositionByBlockId(
  items: BookBlockReadingRecordListItem[]
): Map<number, BookBlockReadingDisposition> {
  const m = new Map<number, BookBlockReadingDisposition>()
  for (const r of items) {
    if (isDisposition(r.status)) {
      m.set(Number(r.bookBlockId), r.status)
    }
  }
  return m
}

export function readBlockIdsFromRecords(
  items: BookBlockReadingRecordListItem[]
): Set<number> {
  const ids = new Set<number>()
  for (const r of items) {
    if (r.status === "READ") {
      ids.add(Number(r.bookBlockId))
    }
  }
  return ids
}

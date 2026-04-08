import type { BookRangeReadingRecordListItem } from "@generated/doughnut-backend-api"
import {
  BOOK_RANGE_READING_STATUS_READ,
  readRangeIdsFromRecords,
} from "@/lib/book-reading/readRangeIdsFromRecords"
import { describe, expect, it } from "vitest"

describe("readRangeIdsFromRecords", () => {
  it("collects bookRangeId for READ rows only", () => {
    const items: BookRangeReadingRecordListItem[] = [
      {
        bookRangeId: "1",
        status: BOOK_RANGE_READING_STATUS_READ,
        completedAt: "2020-01-01T00:00:00Z",
      },
      {
        bookRangeId: "2",
        status: "SKIPPED",
        completedAt: "2020-01-02T00:00:00Z",
      },
    ]
    expect(readRangeIdsFromRecords(items)).toEqual(new Set([1]))
  })

  it("normalizes numeric string ids", () => {
    const items: BookRangeReadingRecordListItem[] = [
      {
        bookRangeId: "42",
        status: BOOK_RANGE_READING_STATUS_READ,
        completedAt: "2020-01-01T00:00:00Z",
      },
    ]
    expect(readRangeIdsFromRecords(items).has(42)).toBe(true)
  })
})

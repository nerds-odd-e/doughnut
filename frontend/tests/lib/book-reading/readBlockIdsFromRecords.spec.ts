import type { BookBlockReadingRecordListItem } from "@generated/doughnut-backend-api"
import { readBlockIdsFromRecords } from "@/lib/book-reading/readBlockIdsFromRecords"
import { describe, expect, it } from "vitest"

describe("readBlockIdsFromRecords", () => {
  it("collects bookBlockId for READ rows only", () => {
    const items: BookBlockReadingRecordListItem[] = [
      {
        bookBlockId: "1",
        status: "READ",
        completedAt: "2020-01-01T00:00:00Z",
      },
      {
        bookBlockId: "2",
        status: "OTHER",
        completedAt: "2020-01-01T00:00:00Z",
      },
    ]
    expect(readBlockIdsFromRecords(items)).toEqual(new Set([1]))
  })

  it("parses string ids as numbers", () => {
    const items: BookBlockReadingRecordListItem[] = [
      {
        bookBlockId: "42",
        status: "READ",
        completedAt: "2020-01-01T00:00:00Z",
      },
    ]
    expect(readBlockIdsFromRecords(items).has(42)).toBe(true)
  })
})

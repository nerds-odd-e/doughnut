import type { BookBlockReadingRecordListItem } from "@generated/doughnut-backend-api"
import {
  readBlockIdsFromRecords,
  readingDispositionByBlockId,
} from "@/lib/book-reading/readBlockIdsFromRecords"
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

describe("readingDispositionByBlockId", () => {
  it("maps READ, SKIMMED, and SKIPPED by bookBlockId", () => {
    const items: BookBlockReadingRecordListItem[] = [
      {
        bookBlockId: "1",
        status: "READ",
        completedAt: "2020-01-01T00:00:00Z",
      },
      {
        bookBlockId: "2",
        status: "SKIMMED",
        completedAt: "2020-01-01T00:00:00Z",
      },
      {
        bookBlockId: "3",
        status: "SKIPPED",
        completedAt: "2020-01-01T00:00:00Z",
      },
    ]
    const m = readingDispositionByBlockId(items)
    expect(m.get(1)).toBe("READ")
    expect(m.get(2)).toBe("SKIMMED")
    expect(m.get(3)).toBe("SKIPPED")
  })

  it("ignores unknown status strings", () => {
    const items: BookBlockReadingRecordListItem[] = [
      {
        bookBlockId: "9",
        status: "OTHER",
        completedAt: "2020-01-01T00:00:00Z",
      },
    ]
    expect(readingDispositionByBlockId(items).size).toBe(0)
  })
})

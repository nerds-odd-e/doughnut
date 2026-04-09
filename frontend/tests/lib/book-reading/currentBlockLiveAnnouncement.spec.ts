import {
  nextLiveAnnouncementText,
  structuralTitleForStartAnchorId,
} from "@/lib/book-reading/currentBlockLiveAnnouncement"
import { describe, expect, it } from "vitest"

const rows = [
  { title: "Part A", startAnchor: { id: 10 } },
  { title: "Part B", startAnchor: { id: 20 } },
  { title: "Part A", startAnchor: { id: 30 } },
]

describe("structuralTitleForStartAnchorId", () => {
  it("returns empty for null anchor", () => {
    expect(structuralTitleForStartAnchorId(null, rows)).toBe("")
  })

  it("returns empty when no row matches", () => {
    expect(structuralTitleForStartAnchorId(999, rows)).toBe("")
  })

  it("returns the matching row title", () => {
    expect(structuralTitleForStartAnchorId(20, rows)).toBe("Part B")
  })
})

describe("nextLiveAnnouncementText", () => {
  it("reports changed when previous is undefined and title is non-empty", () => {
    expect(nextLiveAnnouncementText(undefined, 10, rows)).toEqual({
      text: "Part A",
      changed: true,
    })
  })

  it("reports changed when previous is undefined and anchor is null", () => {
    expect(nextLiveAnnouncementText(undefined, null, rows)).toEqual({
      text: "",
      changed: true,
    })
  })

  it("does not report changed when resolved title equals previous", () => {
    expect(nextLiveAnnouncementText("Part A", 30, rows)).toEqual({
      text: "Part A",
      changed: false,
    })
  })

  it("reports changed when title string changes", () => {
    expect(nextLiveAnnouncementText("Part A", 20, rows)).toEqual({
      text: "Part B",
      changed: true,
    })
  })

  it("reports changed when moving to empty from a title", () => {
    expect(nextLiveAnnouncementText("Part B", null, rows)).toEqual({
      text: "",
      changed: true,
    })
  })

  it("does not report changed when staying empty", () => {
    expect(nextLiveAnnouncementText("", null, rows)).toEqual({
      text: "",
      changed: false,
    })
  })
})

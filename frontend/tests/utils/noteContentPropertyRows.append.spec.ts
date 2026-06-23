import { describe, expect, it } from "vitest"
import {
  appendValueToPropertyRow,
  composeNoteContentFromPropertyRows,
  findPropertyRowIndexByExactKey,
  listPropertyValue,
  parseNoteContentMarkdown,
  propertyRowWithScalar,
  propertyRowsAfterAppendingValueToExactKey,
} from "@/utils/noteContentFrontmatter"

describe("append value to exact property row", () => {
  it("promotes a non-empty scalar row to a two-item list", () => {
    const row = propertyRowWithScalar("example of", "[[A]]")
    expect(appendValueToPropertyRow(row, "[[B]]")).toEqual({
      key: "example of",
      value: listPropertyValue(["[[A]]", "[[B]]"]),
    })
  })

  it("appends to an empty-valued scalar as a single-item list", () => {
    const row = propertyRowWithScalar("example of", "")
    expect(appendValueToPropertyRow(row, "[[B]]")).toEqual({
      key: "example of",
      value: listPropertyValue(["[[B]]"]),
    })
  })

  it("appends to a whitespace-only scalar as a single-item list", () => {
    const row = propertyRowWithScalar("example of", "   ")
    expect(appendValueToPropertyRow(row, "[[B]]")).toEqual({
      key: "example of",
      value: listPropertyValue(["[[B]]"]),
    })
  })

  it("appends to an existing list row", () => {
    const row = {
      key: "example of",
      value: listPropertyValue(["[[A]]", "[[B]]"]),
    }
    expect(appendValueToPropertyRow(row, "[[C]]")).toEqual({
      key: "example of",
      value: listPropertyValue(["[[A]]", "[[B]]", "[[C]]"]),
    })
  })

  it("finds a row by exact trimmed key", () => {
    const rows = [
      propertyRowWithScalar("example of", "one"),
      propertyRowWithScalar("example of 2", "two"),
    ]
    expect(findPropertyRowIndexByExactKey(rows, "example of")).toBe(0)
    expect(findPropertyRowIndexByExactKey(rows, "example of 2")).toBe(1)
    expect(findPropertyRowIndexByExactKey(rows, "missing")).toBe(-1)
  })

  it("returns null when the exact key is absent", () => {
    const rows = [propertyRowWithScalar("example of 2", "two")]
    expect(
      propertyRowsAfterAppendingValueToExactKey(rows, "example of", "new")
    ).toBeNull()
  })

  it("composes appended list values into YAML on the exact key", () => {
    const rows = propertyRowsAfterAppendingValueToExactKey(
      [
        propertyRowWithScalar("example of", "[[A]]"),
        propertyRowWithScalar("example of 2", "[[B]]"),
      ],
      "example of",
      "[[C]]"
    )
    expect(rows).not.toBeNull()
    if (!rows) return
    const md = composeNoteContentFromPropertyRows(rows, "# Body\n")
    const parsed = parseNoteContentMarkdown(md)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties["example of"]).toEqual(
      listPropertyValue(["[[A]]", "[[C]]"])
    )
    expect(parsed.properties["example of 2"]).toEqual(
      propertyRowWithScalar("example of 2", "[[B]]").value
    )
  })
})

import { describe, expect, it } from "vitest"
import {
  composeNoteContentFromPropertyRows,
  insertPropertyRowAt,
  parseNoteContentMarkdown,
  removePropertyRowAt,
  renamePropertyRowKeyAt,
  scalarRecordFromNoteProperties,
  sortedPropertyRowsFromNoteProperties,
  validatePropertyRowsForRichEdit,
} from "@/utils/noteContentFrontmatter"

describe("property rows compose / mutate", () => {
  it("insert: composing first row yields parseable frontmatter with matching scalar props", () => {
    const rows = insertPropertyRowAt([], 0, { key: "topic", value: "training" })
    const md = composeNoteContentFromPropertyRows(rows, "# Hello\n")
    const parsed = parseNoteContentMarkdown(md)
    expect(
      parsed.ok && scalarRecordFromNoteProperties(parsed.properties)
    ).toEqual({
      topic: "training",
    })
    if (parsed.ok) {
      expect(parsed.body).toContain("# Hello")
    }
  })

  it("rename: composed markdown loses old key and exposes new key", () => {
    const parsed = parseNoteContentMarkdown(
      "---\nalpha: one\nbeta: two\n---\nBody\n"
    )
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromNoteProperties(parsed.properties)
    rows = renamePropertyRowKeyAt(rows, 0, "alphaRenamed")
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    const again = parseNoteContentMarkdown(md)
    expect(
      again.ok && scalarRecordFromNoteProperties(again.properties)
    ).toEqual({
      alphaRenamed: "one",
      beta: "two",
    })
    expect(
      again.ok && scalarRecordFromNoteProperties(again.properties).alpha
    ).toBeUndefined()
  })

  it("remove: dropping one row leaves remaining props only", () => {
    const parsed = parseNoteContentMarkdown("---\na: 1\nb: 2\n---\nRest\n")
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromNoteProperties(parsed.properties)
    rows = removePropertyRowAt(rows, 0)
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    const again = parseNoteContentMarkdown(md)
    expect(
      again.ok && scalarRecordFromNoteProperties(again.properties)
    ).toEqual({ b: "2" })
  })

  it("remove: dropping second sorted row keeps first key only", () => {
    const parsed = parseNoteContentMarkdown("---\na: 1\nb: 2\n---\nRest\n")
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromNoteProperties(parsed.properties)
    rows = removePropertyRowAt(rows, 1)
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    const again = parseNoteContentMarkdown(md)
    expect(
      again.ok && scalarRecordFromNoteProperties(again.properties)
    ).toEqual({ a: "1" })
    expect(
      again.ok && scalarRecordFromNoteProperties(again.properties).b
    ).toBeUndefined()
  })

  it("remove: clearing every row yields body-only content without frontmatter fence", () => {
    const parsed = parseNoteContentMarkdown("---\na: 1\nb: 2\n---\nRest\n")
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromNoteProperties(parsed.properties)
    rows = removePropertyRowAt(rows, 0)
    rows = removePropertyRowAt(rows, 0)
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    expect(md).toBe("Rest\n")
    expect(md.startsWith("---")).toBe(false)
    const again = parseNoteContentMarkdown(md)
    expect(
      again.ok && scalarRecordFromNoteProperties(again.properties)
    ).toEqual({})
    if (again.ok) expect(again.body).toBe("Rest\n")
  })

  it("remove: dropping sole row yields omitted frontmatter and unchanged body", () => {
    const parsed = parseNoteContentMarkdown("---\nonly: x\n---\nParagraph.\n")
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    let rows = sortedPropertyRowsFromNoteProperties(parsed.properties)
    rows = removePropertyRowAt(rows, 0)
    const md = composeNoteContentFromPropertyRows(rows, parsed.body)
    expect(md).toBe("Paragraph.\n")
    expect(md.startsWith("---")).toBe(false)
    const again = parseNoteContentMarkdown(md)
    expect(
      again.ok && scalarRecordFromNoteProperties(again.properties)
    ).toEqual({})
    if (again.ok) expect(again.body).toBe("Paragraph.\n")
  })
})

describe("validatePropertyRowsForRichEdit", () => {
  it("accepts distinct keys after trim", () => {
    expect(
      validatePropertyRowsForRichEdit([
        { key: " a ", value: "x" },
        { key: "b", value: " y " },
      ])
    ).toEqual({ ok: true })
  })

  it("allows one draft row with empty key when value is non-empty", () => {
    expect(
      validatePropertyRowsForRichEdit([{ key: "   ", value: "[[Note]]" }])
    ).toEqual({ ok: true })
  })

  it("rejects empty key when value is empty", () => {
    const r = validatePropertyRowsForRichEdit([{ key: "", value: "  " }])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("empty key")
    }
  })

  it("rejects more than one row with empty key", () => {
    const r = validatePropertyRowsForRichEdit([
      { key: "", value: "a" },
      { key: "  ", value: "b" },
    ])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("Only one property")
    }
  })

  it("rejects duplicate keys after trim", () => {
    const r = validatePropertyRowsForRichEdit([
      { key: "same", value: "a" },
      { key: "same", value: "b" },
    ])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("Duplicate")
    }
  })
})

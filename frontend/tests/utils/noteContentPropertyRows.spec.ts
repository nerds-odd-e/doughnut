import { describe, expect, it } from "vitest"
import { AUTHORED_ALIASES_MESSAGE } from "@/utils/authoredAliasesValidation"
import {
  composeNoteContentFromPropertyRows,
  insertPropertyRowAt,
  listPropertyValue,
  parseNoteContentMarkdown,
  propertyRowWithScalar,
  removePropertyRowAt,
  renamePropertyRowKeyAt,
  scalarRecordFromNoteProperties,
  sortedPropertyRowsFromNoteProperties,
  validatePropertyRowsForRichEdit,
} from "@/utils/noteContentFrontmatter"

describe("property rows compose / mutate", () => {
  it("insert: composing first row yields parseable frontmatter with matching scalar props", () => {
    const rows = insertPropertyRowAt(
      [],
      0,
      propertyRowWithScalar("topic", "training")
    )
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

  it("compose preserves list property values from rows", () => {
    const rows = [
      { key: "tags", value: listPropertyValue(["alpha", "beta"]) },
      propertyRowWithScalar("topic", "training"),
    ]
    const md = composeNoteContentFromPropertyRows(rows, "# Body\n")
    const parsed = parseNoteContentMarkdown(md)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.tags).toEqual(listPropertyValue(["alpha", "beta"]))
    expect(scalarRecordFromNoteProperties(parsed.properties)).toEqual({
      topic: "training",
    })
    expect(md).toContain("- alpha")
    expect(md).toContain("- beta")
  })

  it("sortedPropertyRowsFromNoteProperties includes list values", () => {
    const parsed = parseNoteContentMarkdown(`---
tags:
  - a
  - b
topic: x
---
Body`)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    const rows = sortedPropertyRowsFromNoteProperties(parsed.properties)
    expect(rows).toHaveLength(2)
    expect(rows.find((r) => r.key === "tags")?.value).toEqual(
      listPropertyValue(["a", "b"])
    )
    expect(rows.find((r) => r.key === "topic")?.value.kind).toBe("scalar")
  })
})

describe("validatePropertyRowsForRichEdit", () => {
  it("accepts distinct keys after trim", () => {
    expect(
      validatePropertyRowsForRichEdit([
        propertyRowWithScalar(" a ", "x"),
        propertyRowWithScalar("b", " y "),
      ])
    ).toEqual({ ok: true })
  })

  it("allows one draft row with empty key when value is non-empty", () => {
    expect(
      validatePropertyRowsForRichEdit([
        propertyRowWithScalar("   ", "[[Note]]"),
      ])
    ).toEqual({ ok: true })
  })

  it("rejects empty key when scalar value is empty", () => {
    const r = validatePropertyRowsForRichEdit([propertyRowWithScalar("", "  ")])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("empty key")
    }
  })

  it("allows empty key when list value is non-empty", () => {
    expect(
      validatePropertyRowsForRichEdit([
        { key: "", value: listPropertyValue(["draft"]) },
      ])
    ).toEqual({ ok: true })
  })

  it("rejects more than one row with empty key", () => {
    const r = validatePropertyRowsForRichEdit([
      propertyRowWithScalar("", "a"),
      propertyRowWithScalar("  ", "b"),
    ])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("Only one property")
    }
  })

  it("rejects duplicate keys after trim", () => {
    const r = validatePropertyRowsForRichEdit([
      propertyRowWithScalar("same", "a"),
      propertyRowWithScalar("same", "b"),
    ])
    expect(r.ok).toBe(false)
    if (!r.ok) {
      expect(r.message).toContain("Duplicate")
    }
  })

  it("accepts list property rows", () => {
    expect(
      validatePropertyRowsForRichEdit([
        { key: "tags", value: listPropertyValue(["a", "b"]) },
      ])
    ).toEqual({ ok: true })
  })

  it("rejects scalar aliases values", () => {
    const r = validatePropertyRowsForRichEdit([
      propertyRowWithScalar("aliases", "color"),
    ])
    expect(r.ok).toBe(false)
    if (!r.ok) expect(r.message).toBe(AUTHORED_ALIASES_MESSAGE)
  })

  it("accepts valid aliases list rows", () => {
    expect(
      validatePropertyRowsForRichEdit([
        { key: "aliases", value: listPropertyValue(["color", "hue"]) },
      ])
    ).toEqual({ ok: true })
  })

  it("accepts wiki-link overlap alias list rows", () => {
    expect(
      validatePropertyRowsForRichEdit([
        {
          key: "aliases",
          value: listPropertyValue([
            "color",
            "[[Other Note]]",
            "[[Shared Notebook:Hue|display]]",
          ]),
        },
      ])
    ).toEqual({ ok: true })
  })

  it("rejects invalid alias list items", () => {
    const r = validatePropertyRowsForRichEdit([
      { key: "aliases", value: listPropertyValue(["good", "bad|alias"]) },
    ])
    expect(r.ok).toBe(false)
    if (!r.ok) expect(r.message).toBe(AUTHORED_ALIASES_MESSAGE)
  })
})

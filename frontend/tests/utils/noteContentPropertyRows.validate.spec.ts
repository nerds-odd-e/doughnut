import { describe, expect, it } from "vitest"
import { AUTHORED_ALIASES_MESSAGE } from "@/utils/authoredAliasesValidation"
import {
  listPropertyValue,
  propertyRowWithScalar,
  validatePropertyRowsForRichEdit,
} from "@/utils/noteContentFrontmatter"

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

  it("surfaces authored aliases validation for invalid aliases rows", () => {
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
})

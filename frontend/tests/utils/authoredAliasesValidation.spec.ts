import { describe, expect, it } from "vitest"
import {
  AUTHORED_ALIASES_MESSAGE,
  authoredAliasesValidationErrorForPropertyRow,
  authoredAliasesValidationErrorForPropertyValue,
  isAliasesPropertyKey,
} from "@/utils/authoredAliasesValidation"
import { listPropertyValue, scalarPropertyValue } from "@/utils/noteProperties"

describe("isAliasesPropertyKey", () => {
  it("matches aliases case-insensitively with surrounding whitespace", () => {
    expect(isAliasesPropertyKey("aliases")).toBe(true)
    expect(isAliasesPropertyKey(" Aliases ")).toBe(true)
    expect(isAliasesPropertyKey("tags")).toBe(false)
  })
})

describe("authoredAliasesValidationErrorForPropertyValue", () => {
  it("rejects scalar aliases values", () => {
    expect(
      authoredAliasesValidationErrorForPropertyValue(
        scalarPropertyValue("color")
      )
    ).toBe(AUTHORED_ALIASES_MESSAGE)
  })

  it("accepts a valid one-level alias list", () => {
    expect(
      authoredAliasesValidationErrorForPropertyValue(
        listPropertyValue(["color", "hue"])
      )
    ).toBeUndefined()
  })

  it("accepts an empty alias list", () => {
    expect(
      authoredAliasesValidationErrorForPropertyValue(listPropertyValue([]))
    ).toBeUndefined()
  })

  it("rejects blank list items after trim", () => {
    expect(
      authoredAliasesValidationErrorForPropertyValue(
        listPropertyValue(["color", "   "])
      )
    ).toBe(AUTHORED_ALIASES_MESSAGE)
  })

  it("rejects invalid wiki-link characters", () => {
    for (const item of [
      "bad|alias",
      "hash#tag",
      "caret^",
      "colon:",
      "back\\slash",
      "ascii/slash",
      "fullwidth＼",
      "fullwidth／",
      "[[nested",
      "brackets]]",
      "line\nbreak",
    ]) {
      expect(
        authoredAliasesValidationErrorForPropertyValue(
          listPropertyValue([item])
        ),
        item
      ).toBe(AUTHORED_ALIASES_MESSAGE)
    }
  })

  it("allows duplicate alias values", () => {
    expect(
      authoredAliasesValidationErrorForPropertyValue(
        listPropertyValue(["Color", "color"])
      )
    ).toBeUndefined()
  })
})

describe("authoredAliasesValidationErrorForPropertyRow", () => {
  it("ignores non-aliases keys", () => {
    expect(
      authoredAliasesValidationErrorForPropertyRow({
        key: "tags",
        value: scalarPropertyValue("oops"),
      })
    ).toBeUndefined()
  })

  it("validates aliases rows only", () => {
    expect(
      authoredAliasesValidationErrorForPropertyRow({
        key: "aliases",
        value: scalarPropertyValue("color"),
      })
    ).toBe(AUTHORED_ALIASES_MESSAGE)
  })
})

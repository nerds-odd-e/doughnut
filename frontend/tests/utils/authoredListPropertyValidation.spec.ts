import { describe, expect, it } from "vitest"
import {
  authoredListPropertyValidationErrorForPropertyRow,
  authoredListPropertyValidationErrorForPropertyValue,
  isAuthoredListPropertyKey,
} from "@/utils/authoredListPropertyValidation"
import { AUTHORED_ALIASES_MESSAGE } from "@/utils/authoredAliasesValidation"
import { AUTHORED_OVERLAPS_MESSAGE } from "@/utils/authoredOverlapsValidation"
import { listPropertyValue, scalarPropertyValue } from "@/utils/noteProperties"

describe("isAuthoredListPropertyKey", () => {
  it("recognizes aliases and overlaps as authored list keys", () => {
    expect(isAuthoredListPropertyKey("aliases")).toBe(true)
    expect(isAuthoredListPropertyKey("overlaps")).toBe(true)
    expect(isAuthoredListPropertyKey("tags")).toBe(false)
  })
})

describe("authoredListPropertyValidationErrorForPropertyValue", () => {
  it("dispatches aliases and overlaps validation and ignores other keys", () => {
    expect(
      authoredListPropertyValidationErrorForPropertyValue(
        "aliases",
        scalarPropertyValue("color")
      )
    ).toBe(AUTHORED_ALIASES_MESSAGE)
    expect(
      authoredListPropertyValidationErrorForPropertyValue(
        "aliases",
        listPropertyValue(["color"])
      )
    ).toBeUndefined()
    expect(
      authoredListPropertyValidationErrorForPropertyValue(
        "overlaps",
        listPropertyValue(["plain"])
      )
    ).toBe(AUTHORED_OVERLAPS_MESSAGE)
    expect(
      authoredListPropertyValidationErrorForPropertyValue(
        "overlaps",
        listPropertyValue(["[[Other]]"])
      )
    ).toBeUndefined()
    expect(
      authoredListPropertyValidationErrorForPropertyValue(
        "tags",
        scalarPropertyValue("color")
      )
    ).toBeUndefined()
  })
})

describe("authoredListPropertyValidationErrorForPropertyRow", () => {
  it("validates authored list rows only", () => {
    expect(
      authoredListPropertyValidationErrorForPropertyRow({
        key: "tags",
        value: scalarPropertyValue("x"),
      })
    ).toBeUndefined()
    expect(
      authoredListPropertyValidationErrorForPropertyRow({
        key: "aliases",
        value: scalarPropertyValue("x"),
      })
    ).toBe(AUTHORED_ALIASES_MESSAGE)
    expect(
      authoredListPropertyValidationErrorForPropertyRow({
        key: "overlaps",
        value: listPropertyValue(["plain"]),
      })
    ).toBe(AUTHORED_OVERLAPS_MESSAGE)
  })
})

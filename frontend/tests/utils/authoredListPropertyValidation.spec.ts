import { describe, expect, it } from "vitest"
import {
  authoredListPropertyValidationErrorForPropertyRow,
  authoredListPropertyValidationErrorForPropertyValue,
  isAuthoredListPropertyKey,
} from "@/utils/authoredListPropertyValidation"
import { AUTHORED_ALIASES_MESSAGE } from "@/utils/authoredAliasesValidation"
import { listPropertyValue, scalarPropertyValue } from "@/utils/noteProperties"

describe("isAuthoredListPropertyKey", () => {
  it("recognizes aliases as the authored list key today", () => {
    expect(isAuthoredListPropertyKey("aliases")).toBe(true)
    expect(isAuthoredListPropertyKey("tags")).toBe(false)
  })
})

describe("authoredListPropertyValidationErrorForPropertyValue", () => {
  it("dispatches aliases validation and ignores other keys", () => {
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
  })
})

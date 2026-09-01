import { describe, expect, it } from "vitest"
import {
  AUTHORED_OVERLAPS_MESSAGE,
  authoredOverlapsValidationErrorForPropertyRow,
  authoredOverlapsValidationErrorForPropertyValue,
  isOverlapsPropertyKey,
} from "@/utils/authoredOverlapsValidation"
import { listPropertyValue, scalarPropertyValue } from "@/utils/noteProperties"

describe("isOverlapsPropertyKey", () => {
  it("matches overlaps case-insensitively with surrounding whitespace", () => {
    expect(isOverlapsPropertyKey("overlaps")).toBe(true)
    expect(isOverlapsPropertyKey(" Overlaps ")).toBe(true)
    expect(isOverlapsPropertyKey("aliases")).toBe(false)
  })
})

describe("authoredOverlapsValidationErrorForPropertyValue", () => {
  it("rejects scalar overlaps values", () => {
    expect(
      authoredOverlapsValidationErrorForPropertyValue(
        scalarPropertyValue("[[Other]]")
      )
    ).toBe(AUTHORED_OVERLAPS_MESSAGE)
  })

  it("accepts a valid wiki-link-only list", () => {
    expect(
      authoredOverlapsValidationErrorForPropertyValue(
        listPropertyValue([
          "[[Other Note]]",
          "[[Shared Notebook:Hue]]",
          "[[Title|display]]",
        ])
      )
    ).toBeUndefined()
  })

  it("accepts an empty overlaps list", () => {
    expect(
      authoredOverlapsValidationErrorForPropertyValue(listPropertyValue([]))
    ).toBeUndefined()
  })

  it("rejects plain strings and malformed wiki links", () => {
    for (const item of [
      "color",
      "bad|alias",
      "[[",
      "see [[Other]]",
      "[[a]][[b]]",
      "[[]]",
      "   ",
      "/Folder/Title.md",
      "[Title](/Folder/Title.md)",
      "[Title](/Folder/Title.md) extra",
    ]) {
      expect(
        authoredOverlapsValidationErrorForPropertyValue(
          listPropertyValue([item])
        ),
        item
      ).toBe(AUTHORED_OVERLAPS_MESSAGE)
    }
  })
})

describe("authoredOverlapsValidationErrorForPropertyRow", () => {
  it("ignores non-overlaps keys", () => {
    expect(
      authoredOverlapsValidationErrorForPropertyRow({
        key: "aliases",
        value: scalarPropertyValue("oops"),
      })
    ).toBeUndefined()
  })

  it("validates overlaps rows only", () => {
    expect(
      authoredOverlapsValidationErrorForPropertyRow({
        key: "overlaps",
        value: listPropertyValue(["plain"]),
      })
    ).toBe(AUTHORED_OVERLAPS_MESSAGE)
  })
})

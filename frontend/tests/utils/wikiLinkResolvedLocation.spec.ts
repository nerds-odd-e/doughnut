import { describe, expect, it } from "vitest"
import {
  notePropertyHref,
  notePropertyLocation,
  noteShowHref,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  hrefForResolvedWikiTarget,
  locationForResolvedWikiTarget,
} from "@/utils/wikiLinkResolvedLocation"

describe("wikiLinkResolvedLocation", () => {
  it("compiles a note-only target to noteShow", () => {
    expect(locationForResolvedWikiTarget(42, "Moon")).toEqual(
      noteShowLocation(42)
    )
    expect(hrefForResolvedWikiTarget(42, "Moon")).toBe(noteShowHref(42))
  })

  it("compiles a #prop: target to noteProperty using the decoded key", () => {
    const authored = "Moon#prop:a%20part%20of"
    expect(locationForResolvedWikiTarget(42, authored)).toEqual(
      notePropertyLocation(42, "a part of")
    )
    expect(hrefForResolvedWikiTarget(42, authored)).toBe(
      notePropertyHref(42, "a part of")
    )
  })

  it("keeps noteShow when the encoded property key is invalid", () => {
    expect(locationForResolvedWikiTarget(42, "Moon#prop:%ZZ")).toEqual(
      noteShowLocation(42)
    )
  })
})

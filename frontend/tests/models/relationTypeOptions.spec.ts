import { describe, expect, it } from "vitest"
import { relationTypeLabelFromNoteDetails } from "@/models/relationTypeOptions"

describe("relationTypeLabelFromNoteDetails", () => {
  it("returns label for valid relation kebab in frontmatter", () => {
    const md = "---\nrelation: similar-to\ntype: relationship\n---\nbody\n"
    expect(relationTypeLabelFromNoteDetails(md)).toBe("similar to")
  })

  it("matches relation key case-insensitively", () => {
    const md = "---\nRelation: a-part-of\n---\n"
    expect(relationTypeLabelFromNoteDetails(md)).toBe("a part of")
  })

  it("returns undefined when relation key is absent", () => {
    const md = "---\ntype: relationship\n---\n"
    expect(relationTypeLabelFromNoteDetails(md)).toBeUndefined()
  })

  it("returns undefined when relation value is empty or whitespace", () => {
    expect(
      relationTypeLabelFromNoteDetails('---\nrelation: ""\n---\n')
    ).toBeUndefined()
    expect(
      relationTypeLabelFromNoteDetails("---\nrelation: '   '\n---\n")
    ).toBeUndefined()
  })

  it("returns undefined on malformed frontmatter", () => {
    const md = "---\nrelation: similar-to\nstill yaml\n"
    expect(relationTypeLabelFromNoteDetails(md)).toBeUndefined()
  })

  it("returns undefined for null, undefined, or body-only markdown", () => {
    expect(relationTypeLabelFromNoteDetails(null)).toBeUndefined()
    expect(relationTypeLabelFromNoteDetails(undefined)).toBeUndefined()
    expect(
      relationTypeLabelFromNoteDetails("no frontmatter here")
    ).toBeUndefined()
  })
})

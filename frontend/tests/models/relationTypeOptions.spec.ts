import { describe, expect, it } from "vitest"
import { relationTypeLabelFromNoteContent } from "@/models/relationTypeOptions"

describe("relationTypeLabelFromNoteContent", () => {
  it("returns label for valid relation kebab in frontmatter", () => {
    const md = "---\nrelation: similar-to\ntype: relationship\n---\nbody\n"
    expect(relationTypeLabelFromNoteContent(md)).toBe("similar to")
  })

  it("matches relation key case-insensitively", () => {
    const md = "---\nRelation: a-part-of\n---\n"
    expect(relationTypeLabelFromNoteContent(md)).toBe("a part of")
  })

  it("returns undefined when relation key is absent", () => {
    const md = "---\ntype: relationship\n---\n"
    expect(relationTypeLabelFromNoteContent(md)).toBeUndefined()
  })

  it("returns undefined when relation value is empty or whitespace", () => {
    expect(
      relationTypeLabelFromNoteContent('---\nrelation: ""\n---\n')
    ).toBeUndefined()
    expect(
      relationTypeLabelFromNoteContent("---\nrelation: '   '\n---\n")
    ).toBeUndefined()
  })

  it("returns undefined on malformed frontmatter", () => {
    const md = "---\nrelation: similar-to\nstill yaml\n"
    expect(relationTypeLabelFromNoteContent(md)).toBeUndefined()
  })

  it("returns undefined for null, undefined, or body-only markdown", () => {
    expect(relationTypeLabelFromNoteContent(null)).toBeUndefined()
    expect(relationTypeLabelFromNoteContent(undefined)).toBeUndefined()
    expect(
      relationTypeLabelFromNoteContent("no frontmatter here")
    ).toBeUndefined()
  })
})

import { describe, expect, it } from "vitest"
import {
  contentHasRelationProperty,
  propertyRecordHasRelationKey,
  titlePatternFromNoteMarkdown,
} from "@/utils/noteContentFrontmatter"

describe("contentHasRelationProperty", () => {
  it("returns true when relation key exists (case-insensitive)", () => {
    expect(
      contentHasRelationProperty("---\nrelation: parent-of\n---\n\nbody\n")
    ).toBe(true)
    expect(contentHasRelationProperty("---\nRelation: child-of\n---\n\n")).toBe(
      true
    )
  })

  it("returns false without relation key", () => {
    expect(contentHasRelationProperty("---\ntopic: x\n---\n")).toBe(false)
    expect(contentHasRelationProperty("no frontmatter")).toBe(false)
  })

  it("returns false on parse failure", () => {
    expect(contentHasRelationProperty("---\nbad:\n  nested: x\n---\n")).toBe(
      false
    )
  })
})

describe("propertyRecordHasRelationKey", () => {
  it("detects relation with surrounding whitespace on key", () => {
    expect(propertyRecordHasRelationKey({ " relation ": "x" })).toBe(true)
  })
})

describe("titlePatternFromNoteMarkdown", () => {
  it("reads title_pattern and legacy TitlePattern keys", () => {
    expect(
      titlePatternFromNoteMarkdown('---\ntitle_pattern: "{{date}}"\n---\n')
    ).toBe("{{date}}")
    const md = '---\nTitlePattern: "{{date}}"\n---\n'
    expect(titlePatternFromNoteMarkdown(md)).toBe("{{date}}")
  })

  it("returns undefined when key missing or blank", () => {
    expect(titlePatternFromNoteMarkdown("---\na: 1\n---\n")).toBeUndefined()
    expect(
      titlePatternFromNoteMarkdown('---\ntitle_pattern: ""\n---\n')
    ).toBeUndefined()
  })
})

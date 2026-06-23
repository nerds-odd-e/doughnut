import { describe, expect, it } from "vitest"
import {
  contentHasRelationProperty,
  diffFrontmatterPropertyKeyChanges,
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
    expect(
      contentHasRelationProperty("---\nrelation:\n  - parent-of\n---\n")
    ).toBe(false)
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

describe("diffFrontmatterPropertyKeyChanges", () => {
  const md = (yaml: string, body = "") => `---\n${yaml}\n---\n${body}`

  it("returns [] when frontmatter is unchanged", () => {
    const content = md("topic: math\nstatus: active")
    expect(diffFrontmatterPropertyKeyChanges(content, content)).toEqual([])
  })

  it("detects a removed property key", () => {
    const old = md("topic: math\nstatus: active")
    const neu = md("status: active")
    expect(diffFrontmatterPropertyKeyChanges(old, neu)).toEqual([
      { type: "removal", key: "topic" },
    ])
  })

  it("detects a rename when value is unchanged", () => {
    const old = md("topic: math\nstatus: active")
    const neu = md("subject: math\nstatus: active")
    expect(diffFrontmatterPropertyKeyChanges(old, neu)).toEqual([
      { type: "rename", fromKey: "topic", toKey: "subject" },
    ])
  })

  it("treats ambiguous same-value pairs as removals", () => {
    const old = md("topic: math\narea: math\nstatus: active")
    const neu = md("subject: math\nstatus: active")
    expect(diffFrontmatterPropertyKeyChanges(old, neu)).toEqual([
      { type: "removal", key: "topic" },
      { type: "removal", key: "area" },
    ])
  })

  it("returns [] when frontmatter is invalid", () => {
    const valid = md("topic: math")
    const invalid = "---\ntopic: math\n"
    expect(diffFrontmatterPropertyKeyChanges(valid, invalid)).toEqual([])
    expect(diffFrontmatterPropertyKeyChanges(invalid, valid)).toEqual([])
  })
})

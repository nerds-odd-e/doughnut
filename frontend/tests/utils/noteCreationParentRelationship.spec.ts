import { describe, expect, it } from "vitest"
import {
  applyParentRelationshipToCreateContent,
  mergeParentIntoCreateContent,
  parentPropertyScalarFromMarkdown,
  parentRelationshipOptions,
  resolveCreateNoteParentValue,
} from "@/utils/noteCreationParentRelationship"

describe("parentPropertyScalarFromMarkdown", () => {
  it("reads parent from frontmatter", () => {
    expect(
      parentPropertyScalarFromMarkdown('---\nparent: "[[Root]]"\n---\n\nBody')
    ).toBe("[[Root]]")
  })

  it("returns undefined when parent is missing or content empty", () => {
    expect(
      parentPropertyScalarFromMarkdown("---\ntopic: x\n---\n")
    ).toBeUndefined()
    expect(parentPropertyScalarFromMarkdown("")).toBeUndefined()
    expect(parentPropertyScalarFromMarkdown(undefined)).toBeUndefined()
  })
})

describe("parentRelationshipOptions", () => {
  it("omits Same parent when context has no parent", () => {
    expect(parentRelationshipOptions().map((o) => o.value)).toEqual([
      "none",
      "under_current",
    ])
  })

  it("includes Same parent when context has a parent", () => {
    expect(
      parentRelationshipOptions('---\nparent: "[[Root]]"\n---\n').map(
        (o) => o.value
      )
    ).toEqual(["none", "under_current", "same_parent"])
  })
})

describe("resolveCreateNoteParentValue", () => {
  const note = {
    title: "Course intro",
    content: '---\nparent: "[[Root]]"\n---\n',
  }

  it("returns undefined for none", () => {
    expect(resolveCreateNoteParentValue("none", note)).toBeUndefined()
  })

  it("uses current note title for under_current", () => {
    expect(resolveCreateNoteParentValue("under_current", note)).toBe(
      "[[Course intro]]"
    )
  })

  it("copies parent for same_parent", () => {
    expect(resolveCreateNoteParentValue("same_parent", note)).toBe("[[Root]]")
  })
})

describe("mergeParentIntoCreateContent", () => {
  it("returns base content when parent is unset", () => {
    expect(mergeParentIntoCreateContent(undefined, undefined)).toBeUndefined()
    expect(
      mergeParentIntoCreateContent("---\nwikidata_id: Q1\n---\n", undefined)
    ).toBe("---\nwikidata_id: Q1\n---\n")
  })

  it("creates parent-only frontmatter when base is empty", () => {
    const result = mergeParentIntoCreateContent(undefined, "[[Course intro]]")
    expect(result).toContain('parent: "[[Course intro]]"')
  })

  it("preserves wikidata_id when adding parent", () => {
    const result = mergeParentIntoCreateContent(
      "---\nwikidata_id: Q123\n---\n",
      "[[Course intro]]"
    )
    expect(result).toContain("wikidata_id: Q123")
    expect(result).toContain('parent: "[[Course intro]]"')
  })
})

describe("applyParentRelationshipToCreateContent", () => {
  it("leaves content unchanged without a context note", () => {
    expect(
      applyParentRelationshipToCreateContent(
        "---\nwikidata_id: Q1\n---\n",
        "under_current",
        undefined
      )
    ).toBe("---\nwikidata_id: Q1\n---\n")
  })
})

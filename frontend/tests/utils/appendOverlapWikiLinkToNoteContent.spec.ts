import { describe, it, expect } from "vitest"
import { appendOverlapWikiLinkToNoteContent } from "@/utils/appendOverlapWikiLinkToNoteContent"
import { authoredOverlapsValidationErrorForPropertyValue } from "@/utils/authoredOverlapsValidation"
import { isListPropertyValue, listPropertyValue } from "@/utils/noteProperties"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"

function makeTarget(title: string, notebookId: number, notebookName?: string) {
  return { noteTopology: { title }, notebookId, notebookName }
}

function overlapListItems(markdown: string): string[] {
  const parsed = parseNoteContentMarkdown(markdown)
  expect(parsed.ok).toBe(true)
  if (!parsed.ok) return []
  const overlaps = parsed.properties.overlaps
  expect(overlaps).toBeDefined()
  expect(isListPropertyValue(overlaps!)).toBe(true)
  if (!overlaps || !isListPropertyValue(overlaps)) return []
  return [...overlaps.items]
}

describe("appendOverlapWikiLinkToNoteContent", () => {
  it("appends a whole-item wiki-link under overlaps when content has none", () => {
    const result = appendOverlapWikiLinkToNoteContent(
      "## Body\n",
      makeTarget("Sedation", 1, "NB"),
      { notebookId: 1 }
    )

    expect(result).not.toBeNull()
    expect(result).toContain("[[Sedation]]")
    expect(result).not.toContain("|")
    expect(result).not.toMatch(/^aliases:/m)

    const items = overlapListItems(result!)
    expect(items).toContain("[[Sedation]]")
    expect(
      authoredOverlapsValidationErrorForPropertyValue(listPropertyValue(items))
    ).toBeUndefined()
  })

  it("appends a qualified wiki-link for cross-notebook targets", () => {
    const result = appendOverlapWikiLinkToNoteContent(
      "## Body\n",
      makeTarget("Deep Note", 2, "Other NB"),
      { notebookId: 1 }
    )

    expect(result).toContain("[[Other NB:Deep Note]]")
    expect(overlapListItems(result!)).toContain("[[Other NB:Deep Note]]")
  })

  it("merges a wiki-link into an existing overlaps list", () => {
    const markdown = `---
overlaps:
  - "[[Existing]]"
---

# Body`
    const result = appendOverlapWikiLinkToNoteContent(
      markdown,
      makeTarget("Canine", 1, "NB"),
      { notebookId: 1 }
    )

    expect(overlapListItems(result!)).toEqual(["[[Existing]]", "[[Canine]]"])
  })

  it("leaves aliases untouched when appending an overlap", () => {
    const markdown = `---
aliases:
  - puppy
---

# Body`
    const result = appendOverlapWikiLinkToNoteContent(
      markdown,
      makeTarget("Canine", 1, "NB"),
      { notebookId: 1 }
    )

    expect(overlapListItems(result!)).toEqual(["[[Canine]]"])
    const parsed = parseNoteContentMarkdown(result!)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.aliases).toEqual(listPropertyValue(["puppy"]))
  })

  it("returns null when the same wiki-link is already in overlaps", () => {
    const markdown = `---
overlaps:
  - "[[Sedation]]"
---

# Body`
    expect(
      appendOverlapWikiLinkToNoteContent(
        markdown,
        makeTarget("Sedation", 1, "NB"),
        { notebookId: 1 }
      )
    ).toBeNull()
  })

  it("returns null when overlaps is not a YAML list", () => {
    const markdown = `---
overlaps: "[[Sedation]]"
---

# Body`
    expect(
      appendOverlapWikiLinkToNoteContent(
        markdown,
        makeTarget("Canine", 1, "NB"),
        { notebookId: 1 }
      )
    ).toBeNull()
  })
})

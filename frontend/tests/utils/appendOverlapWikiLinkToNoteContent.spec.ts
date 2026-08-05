import { describe, it, expect } from "vitest"
import { appendOverlapWikiLinkToNoteContent } from "@/utils/appendOverlapWikiLinkToNoteContent"
import { authoredAliasesValidationErrorForPropertyValue } from "@/utils/authoredAliasesValidation"
import { isListPropertyValue, listPropertyValue } from "@/utils/noteProperties"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"

function makeTarget(title: string, notebookId: number, notebookName?: string) {
  return { noteTopology: { title }, notebookId, notebookName }
}

function aliasListItems(markdown: string): string[] {
  const parsed = parseNoteContentMarkdown(markdown)
  expect(parsed.ok).toBe(true)
  if (!parsed.ok) return []
  const aliases = parsed.properties.aliases
  expect(aliases).toBeDefined()
  expect(isListPropertyValue(aliases!)).toBe(true)
  if (!aliases || !isListPropertyValue(aliases)) return []
  return [...aliases.items]
}

describe("appendOverlapWikiLinkToNoteContent", () => {
  it("appends a whole-item wiki-link alias when content has no aliases", () => {
    const result = appendOverlapWikiLinkToNoteContent(
      "## Body\n",
      makeTarget("Sedation", 1, "NB"),
      { notebookId: 1 }
    )

    expect(result).not.toBeNull()
    expect(result).toContain("[[Sedation]]")
    expect(result).not.toContain("|")

    const items = aliasListItems(result!)
    expect(items).toContain("[[Sedation]]")
    expect(
      authoredAliasesValidationErrorForPropertyValue(listPropertyValue(items))
    ).toBeUndefined()
  })
})

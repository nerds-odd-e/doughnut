import { appendAliasToNoteContent } from "@/utils/wikidataTitleActions"
import { describe, expect, it } from "vitest"

describe("appendAliasToNoteContent", () => {
  it("writes a YAML aliases list instead of appending to the title", () => {
    expect(appendAliasToNoteContent("## Workshop\n", "Canine")).toBe(
      `---\naliases:\n  - Canine\n---\n## Workshop\n`
    )
  })

  it("preserves existing frontmatter when adding the first aliases list", () => {
    const markdown = `---
wikidata_id: Q11399
---

# Body`
    const result = appendAliasToNoteContent(markdown, "Canine")
    expect(result).toContain("wikidata_id: Q11399")
    expect(result).toContain("aliases:\n  - Canine")
    expect(result).toContain("# Body")
  })

  it("merges a new alias into an existing aliases list", () => {
    const markdown = `---
aliases:
  - puppy
---

# Body`
    expect(appendAliasToNoteContent(markdown, "Canine")).toBe(`---
aliases:
  - puppy
  - Canine
---

# Body`)
  })

  it("dedupes by normalized lookup key when merging", () => {
    const markdown = `---
aliases:
  - Puppy
---

# Body`
    expect(appendAliasToNoteContent(markdown, "puppy")).toBeNull()
  })

  it("dedupes NFKC-normalized aliases when merging", () => {
    const markdown = `---
aliases:
  - Ｃａｎｉｎｅ
---

# Body`
    expect(appendAliasToNoteContent(markdown, "Canine")).toBeNull()
  })

  it("returns null when aliases is not a YAML list", () => {
    const markdown = `---
aliases: puppy
---

# Body`
    expect(appendAliasToNoteContent(markdown, "Canine")).toBeNull()
  })
})

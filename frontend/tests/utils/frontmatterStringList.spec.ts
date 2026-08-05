import { describe, expect, it } from "vitest"
import {
  appendItemToFrontmatterStringList,
  mergeItemIntoStringList,
  normalizedLookupKey,
} from "@/utils/frontmatterStringList"
import { isListPropertyValue } from "@/utils/noteProperties"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"

describe("mergeItemIntoStringList", () => {
  it("appends a new item and dedupes by normalized key", () => {
    expect(mergeItemIntoStringList(["puppy"], "Canine")).toEqual([
      "puppy",
      "Canine",
    ])
    expect(mergeItemIntoStringList(["Canine"], "canine")).toBeNull()
    expect(normalizedLookupKey("Café")).toBe(normalizedLookupKey("café"))
  })
})

describe("appendItemToFrontmatterStringList", () => {
  it("creates or merges into a named list key", () => {
    const created = appendItemToFrontmatterStringList(
      "## Body\n",
      "tags",
      "alpha"
    )
    expect(created).toContain("tags:\n  - alpha")

    const merged = appendItemToFrontmatterStringList(
      `---
tags:
  - alpha
---

# Body`,
      "tags",
      "beta"
    )
    expect(merged).not.toBeNull()
    const parsed = parseNoteContentMarkdown(merged!)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(isListPropertyValue(parsed.properties.tags!)).toBe(true)
    if (!isListPropertyValue(parsed.properties.tags!)) return
    expect(parsed.properties.tags.items).toEqual(["alpha", "beta"])
  })
})

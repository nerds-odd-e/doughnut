import { describe, expect, it } from "vitest"
import { migrateLegacyAliasWikiLinksToOverlaps } from "@/utils/migrateLegacyAliasWikiLinksToOverlaps"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"
import { isListPropertyValue } from "@/utils/noteProperties"

describe("migrateLegacyAliasWikiLinksToOverlaps", () => {
  it("moves wiki-link alias items into overlaps and keeps plain aliases", () => {
    const input = `---
aliases:
  - color
  - "[[Partner]]"
---
Colour means a hue
`
    const migrated = migrateLegacyAliasWikiLinksToOverlaps(input)
    expect(migrated).not.toBeNull()
    const parsed = parseNoteContentMarkdown(migrated!)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    const aliases = parsed.properties.aliases
    const overlaps = parsed.properties.overlaps
    expect(aliases && isListPropertyValue(aliases) && aliases.items).toEqual([
      "color",
    ])
    expect(overlaps && isListPropertyValue(overlaps) && overlaps.items).toEqual(
      ["[[Partner]]"]
    )
  })

  it("returns null when aliases have no wiki-link items", () => {
    const input = `---
aliases:
  - color
---
body
`
    expect(migrateLegacyAliasWikiLinksToOverlaps(input)).toBeNull()
  })

  it("merges into existing overlaps without duplicating", () => {
    const input = `---
aliases:
  - "[[Partner]]"
overlaps:
  - "[[Partner]]"
  - "[[Other]]"
---
body
`
    const migrated = migrateLegacyAliasWikiLinksToOverlaps(input)
    expect(migrated).not.toBeNull()
    const parsed = parseNoteContentMarkdown(migrated!)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.aliases).toBeUndefined()
    const overlaps = parsed.properties.overlaps
    expect(overlaps && isListPropertyValue(overlaps) && overlaps.items).toEqual(
      ["[[Partner]]", "[[Other]]"]
    )
  })
})

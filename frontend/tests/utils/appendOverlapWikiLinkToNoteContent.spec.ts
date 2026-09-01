import { describe, it, expect, vi, beforeEach } from "vitest"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { wikiLinkFromAuthoredToken } from "@/utils/authoredLinkMarkup"
import { appendOverlapWikiLinkToNoteContent } from "@/utils/appendOverlapWikiLinkToNoteContent"
import { authoredOverlapsValidationErrorForPropertyValue } from "@/utils/authoredOverlapsValidation"
import { isListPropertyValue, listPropertyValue } from "@/utils/noteProperties"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"

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
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("appends the backend-authored folder-qualified Portable path under overlaps", async () => {
    mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Other NB/Nested/Deep Note",
    })

    const result = await appendOverlapWikiLinkToNoteContent(
      "## Body\n",
      1,
      2,
      []
    )

    expect(result).toContain("[[Other NB/Nested/Deep Note]]")
    expect(result).not.toContain("|")
    expect(result).not.toMatch(/^aliases:/m)
    expect(overlapListItems(result!)).toContain("[[Other NB/Nested/Deep Note]]")
    expect(
      authoredOverlapsValidationErrorForPropertyValue(
        listPropertyValue(["[[Other NB/Nested/Deep Note]]"])
      )
    ).toBeUndefined()
  })

  it("calls the authoring operation with the source and destination note ids", async () => {
    const spy = mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Sedation",
    })

    await appendOverlapWikiLinkToNoteContent("## Body\n", 7, 42, [])

    expect(spy).toHaveBeenCalledWith({
      path: { note: 7 },
      query: { destinationNote: 42 },
    })
  })

  it("merges a wiki-link into an existing overlaps list", async () => {
    mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Canine",
    })
    const markdown = `---
overlaps:
  - "[[Existing]]"
---

# Body`
    const result = await appendOverlapWikiLinkToNoteContent(markdown, 1, 2, [])

    expect(overlapListItems(result!)).toEqual(["[[Existing]]", "[[Canine]]"])
  })

  it("leaves aliases untouched when appending an overlap", async () => {
    mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Canine",
    })
    const markdown = `---
aliases:
  - puppy
---

# Body`
    const result = await appendOverlapWikiLinkToNoteContent(markdown, 1, 2, [])

    expect(overlapListItems(result!)).toEqual(["[[Canine]]"])
    const parsed = parseNoteContentMarkdown(result!)
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.aliases).toEqual(listPropertyValue(["puppy"]))
  })

  it("returns null when overlaps already names the destination via a different spelling", async () => {
    mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Sedation",
    })
    const markdown = `---
overlaps:
  - "[[Folder/Sedation]]"
---

# Body`
    expect(
      await appendOverlapWikiLinkToNoteContent(markdown, 1, 2, [
        wikiLinkFromAuthoredToken("Folder/Sedation", 2),
      ])
    ).toBeNull()
  })

  it("returns null when overlaps is not a YAML list", async () => {
    mockSdkService(NoteController, "authoredPortablePath", {
      portablePath: "Canine",
    })
    const markdown = `---
overlaps: "[[Sedation]]"
---

# Body`
    expect(
      await appendOverlapWikiLinkToNoteContent(markdown, 1, 2, [])
    ).toBeNull()
  })
})

import { describe, expect, it } from "vitest"
import { notePropertyHref, noteShowHref } from "@/routes/noteShowLocation"
import {
  countMarkdownLinksAndImagesInNoteContent,
  stripMarkdownLinksAndImagesInNoteContent,
} from "@/utils/stripPastedMarkdownLinks"

describe("stripMarkdownLinksAndImagesInNoteContent", () => {
  it("leaves verbatim frontmatter untouched when stripping links in the body", () => {
    const markdown =
      "---\nalpha: one\nbeta: 2\n---\nSee [a](https://example.com)\n"
    expect(
      stripMarkdownLinksAndImagesInNoteContent(markdown, true, false)
    ).toBe("---\nalpha: one\nbeta: 2\n---\nSee a")
  })

  it("does not count markdown links that only appear inside frontmatter YAML", () => {
    const markdown =
      '---\nsummary: "[hidden](https://a.com)"\n---\nPlain body.\n'
    expect(countMarkdownLinksAndImagesInNoteContent(markdown)).toEqual({
      linkCount: 0,
      imageCount: 0,
    })
  })

  it("keeps [[wiki]] syntax and strips ordinary markdown links to their labels", () => {
    const md = `[[Alpha]] and [Beta](/n99) [shown](${notePropertyHref(99, "topic")}) then [x](https://z.test)`
    expect(stripMarkdownLinksAndImagesInNoteContent(md, true, false)).toBe(
      "[[Alpha]] and Beta shown then x"
    )
  })

  it("counts note-show and note-property URLs as ordinary links for the paste choice", () => {
    expect(
      countMarkdownLinksAndImagesInNoteContent(
        `[shown](${notePropertyHref(99, "topic")}) [B](https://b) [C](${noteShowHref(2)})`
      )
    ).toEqual({ linkCount: 3, imageCount: 0 })
  })
})

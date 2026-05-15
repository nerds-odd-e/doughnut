import { describe, expect, it } from "vitest"
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

  it("keeps [[wiki]] syntax and turns internal note href links into wiki links", () => {
    const md = "[[Alpha]] and [Beta](/d/n/99) then [x](https://z.test)"
    expect(stripMarkdownLinksAndImagesInNoteContent(md, true, false)).toBe(
      "[[Alpha]] and [[Beta]] then x"
    )
  })

  it("treats compact /n<id> markdown hrefs as internal note links", () => {
    const md = "[Beta](/n99) then [x](https://z.test)"
    expect(stripMarkdownLinksAndImagesInNoteContent(md, true, false)).toBe(
      "[[Beta]] then x"
    )
  })

  it("counts only non-internal-note links for the paste options prompt", () => {
    expect(
      countMarkdownLinksAndImagesInNoteContent(
        "[A](/d/n/1) [B](https://b) [C](/d/n/2)"
      )
    ).toEqual({ linkCount: 1, imageCount: 0 })
  })

  it("treats absolute note-show URLs as wiki links when stripping", () => {
    const md = "[Same](https://app.example/d/n/3) end"
    expect(stripMarkdownLinksAndImagesInNoteContent(md, true, false)).toBe(
      "[[Same]] end"
    )
  })
})

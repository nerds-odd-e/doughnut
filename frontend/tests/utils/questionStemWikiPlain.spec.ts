import { describe, expect, it } from "vitest"
import markdownizer from "@/components/form/markdownizer"
import { replaceWellFormedWikiLinksWithDisplayPlain } from "@/utils/questionStemWikiPlain"

describe("replaceWellFormedWikiLinksWithDisplayPlain", () => {
  it("unpiped link uses full inner as visible text", () => {
    expect(
      replaceWellFormedWikiLinksWithDisplayPlain("See [[Alpha]] here")
    ).toBe("See Alpha here")
  })

  it("piped link uses display side", () => {
    expect(
      replaceWellFormedWikiLinksWithDisplayPlain(
        "x [[LinkTarget|friendly label]] y"
      )
    ).toBe("x friendly label y")
  })

  it("replaces multiple links on one line", () => {
    expect(
      replaceWellFormedWikiLinksWithDisplayPlain("[[A]] and [[B|bee]]")
    ).toBe("A and bee")
  })

  it("leaves unclosed brackets unchanged", () => {
    expect(replaceWellFormedWikiLinksWithDisplayPlain("a [[open only")).toBe(
      "a [[open only"
    )
  })

  it("leaves empty bracket inner unchanged", () => {
    expect(replaceWellFormedWikiLinksWithDisplayPlain("a [[ ]] b")).toBe(
      "a [[ ]] b"
    )
  })

  it("preserves cloze mark markup and still strips wikilinks", () => {
    const md = `<mark title='Hidden text that is matching the answer'>[...]</mark> uses [[T|shown]] end`
    expect(replaceWellFormedWikiLinksWithDisplayPlain(md)).toBe(
      `<mark title='Hidden text that is matching the answer'>[...]</mark> uses shown end`
    )
  })

  it("markdownToHtml still renders mark-wrapped cloze after wiki strip", () => {
    const md = `<mark title='Hidden text that is matching the answer'>[...]</mark> uses [[T|shown]] end`
    const html = markdownizer.markdownToHtml(
      replaceWellFormedWikiLinksWithDisplayPlain(md)
    )
    expect(html).toContain("mark")
    expect(html).toContain("shown")
    expect(html).not.toContain("[[")
  })
})

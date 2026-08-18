import { hasNewWikiLinkTexts } from "@/utils/noteContentWikiLinks"
import { describe, expect, it } from "vitest"

describe("hasNewWikiLinkTexts", () => {
  it("is true when next introduces a new link text", () => {
    expect(hasNewWikiLinkTexts("", "See [[Foo]]")).toBe(true)
  })

  it("is false when only existing link texts appear", () => {
    expect(hasNewWikiLinkTexts("[[Foo]]", "[[Foo]] more text")).toBe(false)
  })

  it("is true when a second distinct link appears", () => {
    expect(hasNewWikiLinkTexts("[[A]]", "[[A]] [[B]]")).toBe(true)
  })

  it("is true when link inner text changes", () => {
    expect(hasNewWikiLinkTexts("[[Old]]", "[[New]]")).toBe(true)
  })

  it("is true when next introduces a path-Markdown link", () => {
    expect(hasNewWikiLinkTexts("", "[Moon](/Moon.md)")).toBe(true)
  })

  it("is false when only the same path-Markdown token appears", () => {
    expect(
      hasNewWikiLinkTexts("[Moon](/Moon.md)", "[Moon](/Moon.md) more")
    ).toBe(false)
  })

  it("is true when a path-Markdown token appears next to an existing wiki token", () => {
    expect(hasNewWikiLinkTexts("[[Moon]]", "[[Moon]] [Moon](/Moon.md)")).toBe(
      true
    )
  })

  it("is false for a bare path that is not a Markdown token", () => {
    expect(hasNewWikiLinkTexts("", "/Moon.md")).toBe(false)
  })
})

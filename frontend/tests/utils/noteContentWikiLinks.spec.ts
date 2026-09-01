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

  it("is false when next introduces only a Markdown URL", () => {
    expect(hasNewWikiLinkTexts("", "[Moon](/Moon.md)")).toBe(false)
  })

  it("is false for a bare path that is not a Markdown token", () => {
    expect(hasNewWikiLinkTexts("", "/Moon.md")).toBe(false)
  })
})

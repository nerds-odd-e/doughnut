import {
  extractWikiLinkTexts,
  hasNewWikiLinkTexts,
} from "@/utils/noteDetailsWikiLinks"
import { describe, expect, it } from "vitest"

describe("noteDetailsWikiLinks", () => {
  describe("extractWikiLinkTexts", () => {
    it("collects inner text of complete wiki links", () => {
      expect(
        [...extractWikiLinkTexts("See [[Foo]] and [[Bar]]")].sort()
      ).toEqual(["Bar", "Foo"])
    })

    it("trims inner whitespace", () => {
      expect([...extractWikiLinkTexts("[[  Baz  ]]")]).toEqual(["Baz"])
    })

    it("ignores incomplete brackets", () => {
      expect(extractWikiLinkTexts("[[open")).toEqual(new Set())
    })
  })

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
  })
})

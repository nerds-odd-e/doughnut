import {
  epubSpinePathMatches,
  resolveSpineHrefForStoredPath,
  splitEpubHref,
} from "@/lib/book-reading/epubHrefMatch"
import { describe, expect, it } from "vitest"

describe("splitEpubHref", () => {
  it("splits path and fragment", () => {
    expect(splitEpubHref("OEBPS/ch.xhtml#id1")).toEqual({
      path: "OEBPS/ch.xhtml",
      fragment: "id1",
    })
  })

  it("returns null fragment when hash only", () => {
    expect(splitEpubHref("path.xhtml#")).toEqual({
      path: "path.xhtml",
      fragment: null,
    })
  })

  it("returns whole string as path when no hash", () => {
    expect(splitEpubHref("OEBPS/ch.xhtml")).toEqual({
      path: "OEBPS/ch.xhtml",
      fragment: null,
    })
  })
})

describe("epubSpinePathMatches", () => {
  it("matches exact paths", () => {
    expect(epubSpinePathMatches("a/b.xhtml", "a/b.xhtml")).toBe(true)
  })

  it("matches package-root to manifest-relative", () => {
    expect(epubSpinePathMatches("OEBPS/chapter3.xhtml", "chapter3.xhtml")).toBe(
      true
    )
    expect(epubSpinePathMatches("chapter3.xhtml", "OEBPS/chapter3.xhtml")).toBe(
      true
    )
  })

  it("rejects unrelated paths", () => {
    expect(epubSpinePathMatches("OEBPS/a.xhtml", "other/b.xhtml")).toBe(false)
  })
})

describe("resolveSpineHrefForStoredPath", () => {
  const spine = [
    { href: "chapter1.xhtml" },
    { href: "chapter2.xhtml" },
    { href: "chapter3.xhtml" },
  ]

  it("returns the spine href when package-root path matches a manifest-relative section", () => {
    expect(resolveSpineHrefForStoredPath(spine, "OEBPS/chapter3.xhtml")).toBe(
      "chapter3.xhtml"
    )
  })

  it("returns the spine href when stored path already matches a section href", () => {
    expect(resolveSpineHrefForStoredPath(spine, "chapter2.xhtml")).toBe(
      "chapter2.xhtml"
    )
  })

  it("returns null when no section matches", () => {
    expect(resolveSpineHrefForStoredPath(spine, "other/missing.xhtml")).toBe(
      null
    )
  })

  it("returns null when spine is missing or empty", () => {
    expect(resolveSpineHrefForStoredPath(undefined, "OEBPS/ch.xhtml")).toBe(
      null
    )
    expect(resolveSpineHrefForStoredPath([], "OEBPS/ch.xhtml")).toBe(null)
  })
})

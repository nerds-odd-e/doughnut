import { describe, expect, it } from "vitest"
import {
  decodeWikiLinkPropertyKey,
  encodeWikiLinkPropertyKey,
  formatWikiLinkAuthoredTarget,
  parseWikiLinkAuthoredTarget,
  withWikiLinkAuthoredNoteTarget,
} from "@/utils/wikiLinkAuthoredTarget"

/**
 * Shared fixture table with WikiLinkAuthoredTargetTest (spaces, Unicode, `/`,
 * `%`, `|`, `]`, `?`, `#`, mixed-case, unreserved). ADR 0004: UTF-8 `%HH`,
 * unreserved literal, product uppercase hex.
 */
const encodedPropertyKeyPairs: ReadonlyArray<readonly [string, string]> = [
  ["a part of", "a%20part%20of"],
  ["月", "%E6%9C%88"],
  ["a/b", "a%2Fb"],
  ["100%", "100%25"],
  ["a|b", "a%7Cb"],
  ["a]b", "a%5Db"],
  ["a?b", "a%3Fb"],
  ["a#b", "a%23b"],
  ["WikiData", "WikiData"],
  ["Az09-._~", "Az09-._~"],
]

describe("wikiLinkAuthoredTarget", () => {
  it.each(encodedPropertyKeyPairs)(
    "encodePropertyKey uses RFC 3986 unreserved and uppercase hex: %s → %s",
    (yamlKey, encoded) => {
      expect(encodeWikiLinkPropertyKey(yamlKey)).toBe(encoded)
    }
  )

  it.each(encodedPropertyKeyPairs)(
    "decodePropertyKey round-trips product encoding: %s ← %s",
    (yamlKey, encoded) => {
      expect(decodeWikiLinkPropertyKey(encoded)).toBe(yamlKey)
    }
  )

  it("decodePropertyKey accepts lowercase hex", () => {
    expect(decodeWikiLinkPropertyKey("a%2fb")).toBe("a/b")
  })

  it("decodePropertyKey rejects invalid escape", () => {
    expect(decodeWikiLinkPropertyKey("%")).toBeUndefined()
    expect(decodeWikiLinkPropertyKey("%2")).toBeUndefined()
    expect(decodeWikiLinkPropertyKey("%ZZ")).toBeUndefined()
  })

  it("decodePropertyKey rejects invalid UTF-8", () => {
    expect(decodeWikiLinkPropertyKey("%80")).toBeUndefined()
  })

  it("decodePropertyKey rejects empty encoded component", () => {
    expect(decodeWikiLinkPropertyKey("")).toBeUndefined()
    expect(decodeWikiLinkPropertyKey(undefined)).toBeUndefined()
  })

  it("parse of a note-only target has no property suffix", () => {
    const parsed = parseWikiLinkAuthoredTarget("Moon")
    expect(parsed.noteTarget).toBe("Moon")
    expect(parsed.encodedPropertyKey).toBeUndefined()
    expect(formatWikiLinkAuthoredTarget(parsed)).toBe("Moon")
  })

  it("parse splits on the first #prop: separator", () => {
    const parsed = parseWikiLinkAuthoredTarget("Moon#prop:a%20part%20of")
    expect(parsed.noteTarget).toBe("Moon")
    expect(parsed.encodedPropertyKey).toBe("a%20part%20of")
    expect(formatWikiLinkAuthoredTarget(parsed)).toBe("Moon#prop:a%20part%20of")
  })

  it("parse keeps qualified and path-shaped note targets before the separator", () => {
    expect(
      parseWikiLinkAuthoredTarget("Sky:Moon#prop:a%20part%20of").noteTarget
    ).toBe("Sky:Moon")
    expect(
      parseWikiLinkAuthoredTarget("/Solar/Moon.md#prop:a%20part%20of")
        .noteTarget
    ).toBe("/Solar/Moon.md")
  })

  it("parse of a title containing literal #prop: cannot be the sole unqualified target", () => {
    const parsed = parseWikiLinkAuthoredTarget("Foo#prop:bar")
    expect(parsed.noteTarget).toBe("Foo")
    expect(parsed.encodedPropertyKey).toBe("bar")
  })

  it("parse splits on the first marker when the encoded component contains another", () => {
    const parsed = parseWikiLinkAuthoredTarget("Foo#prop:bar#prop:baz")
    expect(parsed.noteTarget).toBe("Foo")
    expect(parsed.encodedPropertyKey).toBe("bar#prop:baz")
  })

  it("withNoteTarget preserves the encoded property suffix", () => {
    const rewritten = withWikiLinkAuthoredNoteTarget(
      parseWikiLinkAuthoredTarget("Moon#prop:a%20part%20of"),
      "Luna"
    )
    expect(formatWikiLinkAuthoredTarget(rewritten)).toBe(
      "Luna#prop:a%20part%20of"
    )
  })

  it("format from a decoded key uses product encoding", () => {
    const target = {
      noteTarget: "Moon",
      encodedPropertyKey: encodeWikiLinkPropertyKey("a part of"),
    }
    expect(formatWikiLinkAuthoredTarget(target)).toBe("Moon#prop:a%20part%20of")
    expect(decodeWikiLinkPropertyKey(target.encodedPropertyKey)).toBe(
      "a part of"
    )
  })

  it("decoded property key is empty when the encoded component is invalid", () => {
    expect(
      decodeWikiLinkPropertyKey(
        parseWikiLinkAuthoredTarget("Moon#prop:%ZZ").encodedPropertyKey
      )
    ).toBeUndefined()
  })

  it("has a property suffix when the separator is present even if the encoded key is empty", () => {
    const parsed = parseWikiLinkAuthoredTarget("Moon#prop:")
    expect(parsed.encodedPropertyKey).toBe("")
    expect(parsed.encodedPropertyKey).not.toBeUndefined()
    expect(decodeWikiLinkPropertyKey(parsed.encodedPropertyKey)).toBeUndefined()
  })
})

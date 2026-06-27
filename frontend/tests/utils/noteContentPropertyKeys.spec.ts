import { describe, expect, it } from "vitest"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"
import {
  isExampleOfPropertyKey,
  isImagePropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  nextAvailablePropertyKeyForBase,
  nextAvailablePropertyKeyForPreset,
  propertyKeyBaseAndSuffix,
  richModeKeyDropdownPresetKeys,
  richModeKeyDropdownPresetKeysForPropertyRows,
} from "@/utils/noteContentPropertyKeys"

describe("propertyKeyBaseAndSuffix", () => {
  it("parses bare and suffixed keys", () => {
    expect(propertyKeyBaseAndSuffix("url")).toEqual({
      base: "url",
      suffix: null,
    })
    expect(propertyKeyBaseAndSuffix("url 2")).toEqual({
      base: "url",
      suffix: 2,
    })
    expect(propertyKeyBaseAndSuffix("example of 2")).toEqual({
      base: "example of",
      suffix: 2,
    })
  })
})

describe("nextAvailablePropertyKeyForBase", () => {
  it("returns the base key when the family is unused", () => {
    expect(nextAvailablePropertyKeyForBase("a part of", [])).toBe("a part of")
  })

  it("returns the next suffixed key when the base is taken", () => {
    expect(nextAvailablePropertyKeyForBase("a part of", ["a part of"])).toBe(
      "a part of 2"
    )
    expect(
      nextAvailablePropertyKeyForBase("a part of", ["a part of", "a part of 2"])
    ).toBe("a part of 3")
  })

  it("treats existing keys case-insensitively", () => {
    expect(nextAvailablePropertyKeyForBase("a part of", ["A part of"])).toBe(
      "a part of 2"
    )
  })
})

describe("nextAvailablePropertyKeyForPreset", () => {
  it("returns the canonical preset when the family is unused", () => {
    expect(nextAvailablePropertyKeyForPreset("url", [])).toBe("url")
  })

  it("returns the next suffixed key when the base is taken", () => {
    expect(
      nextAvailablePropertyKeyForPreset("aliases", [
        propertyRowWithScalar("aliases", "color"),
      ])
    ).toBe("aliases 2")
    expect(
      nextAvailablePropertyKeyForPreset("url", [
        propertyRowWithScalar("url", "https://a"),
      ])
    ).toBe("url 2")
    expect(
      nextAvailablePropertyKeyForPreset("url", [
        propertyRowWithScalar("url", "https://a"),
        propertyRowWithScalar("url 2", "https://b"),
      ])
    ).toBe("url 3")
  })

  it("treats wikidataId as occupying the wikidata_id family", () => {
    expect(
      nextAvailablePropertyKeyForPreset("wikidata_id", [
        propertyRowWithScalar("wikidataId", "Q1"),
      ])
    ).toBe("wikidata_id 2")
  })

  it("excludes the current row when computing the next key", () => {
    const rows = [propertyRowWithScalar("url", "https://a")]
    expect(
      nextAvailablePropertyKeyForPreset("url", rows, { excludeRowIndex: 0 })
    ).toBe("url")
  })
})

describe("richModeKeyDropdownPresetKeysForPropertyRows", () => {
  it("matches full list when no rows use preset families", () => {
    expect(richModeKeyDropdownPresetKeysForPropertyRows(false, [])).toEqual(
      richModeKeyDropdownPresetKeys(false)
    )
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("status", "ok"),
      ])
    ).toEqual(richModeKeyDropdownPresetKeys(false))
  })

  it("resolves occupied presets to the next suffixed key", () => {
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("image", "/a.png"),
      ])
    ).toEqual([
      "aliases",
      "image 2",
      "wikidata_id",
      "url",
      "example of",
      "question_generation_instruction",
    ])
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("wikidataId", "Q1"),
      ])
    ).toEqual([
      "aliases",
      "image",
      "wikidata_id 2",
      "url",
      "example of",
      "question_generation_instruction",
    ])
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("url", "https://x"),
      ])
    ).toEqual([
      "aliases",
      "image",
      "wikidata_id",
      "url 2",
      "example of",
      "question_generation_instruction",
    ])
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("example of", "[[A]]"),
        propertyRowWithScalar("example of 2", "[[B]]"),
      ])
    ).toEqual([
      "aliases",
      "image",
      "wikidata_id",
      "url",
      "example of 3",
      "question_generation_instruction",
    ])
  })

  it("ignores rows with empty keys", () => {
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("", "x"),
        propertyRowWithScalar("  ", "y"),
      ])
    ).toEqual(richModeKeyDropdownPresetKeys(false))
  })
})

describe("richModeKeyDropdownPresetKeys", () => {
  it("includes question_generation_instruction in default presets", () => {
    expect(richModeKeyDropdownPresetKeys(false)).toEqual([
      "aliases",
      "image",
      "wikidata_id",
      "url",
      "example of",
      "question_generation_instruction",
    ])
  })

  it("appends index-only keys when isIndexContext is true", () => {
    expect(richModeKeyDropdownPresetKeys(true)).toEqual([
      "aliases",
      "image",
      "wikidata_id",
      "url",
      "example of",
      "question_generation_instruction",
      "title_pattern",
    ])
  })
})

describe("isImagePropertyKey", () => {
  it("matches image case-insensitively with trim and suffix", () => {
    expect(isImagePropertyKey("image")).toBe(true)
    expect(isImagePropertyKey(" Image ")).toBe(true)
    expect(isImagePropertyKey("IMAGE")).toBe(true)
    expect(isImagePropertyKey("image 2")).toBe(true)
    expect(isImagePropertyKey("image_mask")).toBe(false)
    expect(isImagePropertyKey("imagery")).toBe(false)
  })
})

describe("isUrlPropertyKey", () => {
  it("matches url with varied casing, spacing, and suffix", () => {
    expect(isUrlPropertyKey("url")).toBe(true)
    expect(isUrlPropertyKey("  URL ")).toBe(true)
    expect(isUrlPropertyKey("url 3")).toBe(true)
    expect(isUrlPropertyKey("urls")).toBe(false)
    expect(isUrlPropertyKey("wikidata_id")).toBe(false)
  })
})

describe("isWikidataIdPropertyKey", () => {
  it("matches wikidata_id family including aliases and suffix", () => {
    expect(isWikidataIdPropertyKey("wikidata_id")).toBe(true)
    expect(isWikidataIdPropertyKey("  WikiData_ID ")).toBe(true)
    expect(isWikidataIdPropertyKey("wikidataId")).toBe(true)
    expect(isWikidataIdPropertyKey("wikidata_id 2")).toBe(true)
    expect(isWikidataIdPropertyKey("wikidataId 2")).toBe(true)
    expect(isWikidataIdPropertyKey("relation")).toBe(false)
    expect(isWikidataIdPropertyKey("wikidata")).toBe(false)
  })
})

describe("isExampleOfPropertyKey", () => {
  it("matches example of and suffixed variants", () => {
    expect(isExampleOfPropertyKey("example of")).toBe(true)
    expect(isExampleOfPropertyKey("Example Of 2")).toBe(true)
    expect(isExampleOfPropertyKey("example")).toBe(false)
  })
})

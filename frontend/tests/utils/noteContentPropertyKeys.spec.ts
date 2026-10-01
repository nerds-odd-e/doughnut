import { describe, expect, it } from "vitest"
import {
  isExampleOfPropertyKey,
  isImagePropertyKey,
  isNoteLevelPropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  propertyKeyBaseAndSuffix,
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

describe("isNoteLevelPropertyKey", () => {
  it("matches note_level including aliases and suffix", () => {
    expect(isNoteLevelPropertyKey("note_level")).toBe(true)
    expect(isNoteLevelPropertyKey("Note_Level")).toBe(true)
    expect(isNoteLevelPropertyKey("noteLevel")).toBe(true)
    expect(isNoteLevelPropertyKey("note_level 2")).toBe(true)
    expect(isNoteLevelPropertyKey("topic")).toBe(false)
  })
})

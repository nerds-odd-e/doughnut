import { describe, expect, it } from "vitest"
import {
  richModeKeyDropdownPresetKeys,
  richModeKeyDropdownPresetKeysForPropertyRows,
} from "@/utils/noteContentPropertyKeyPresets"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"

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
    const defaults = richModeKeyDropdownPresetKeys(false)
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("image", "/a.png"),
      ])
    ).toEqual(defaults.map((k) => (k === "image" ? "image 2" : k)))
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("wikidataId", "Q1"),
      ])
    ).toEqual(defaults.map((k) => (k === "wikidata_id" ? "wikidata_id 2" : k)))
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("url", "https://x"),
      ])
    ).toEqual(defaults.map((k) => (k === "url" ? "url 2" : k)))
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("example of", "[[A]]"),
        propertyRowWithScalar("example of 2", "[[B]]"),
      ])
    ).toEqual(defaults.map((k) => (k === "example of" ? "example of 3" : k)))
  })

  it("omits occupied aliases and overlaps instead of suggesting a suffixed key", () => {
    const defaults = richModeKeyDropdownPresetKeys(false)
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("aliases", "color"),
      ])
    ).toEqual(defaults.filter((k) => k !== "aliases"))
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("overlaps", "[[Other]]"),
      ])
    ).toEqual(defaults.filter((k) => k !== "overlaps"))
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
  it("returns the default rich-mode preset keys", () => {
    expect(richModeKeyDropdownPresetKeys(false)).toEqual([
      "aliases",
      "overlaps",
      "image",
      "wikidata_id",
      "url",
      "example of",
      "question_generation_instruction",
    ])
  })

  it("omits aliases and overlaps and appends readme-only keys for folder and notebook readme", () => {
    expect(richModeKeyDropdownPresetKeys(true)).toEqual([
      "image",
      "wikidata_id",
      "url",
      "example of",
      "question_generation_instruction",
      "title_pattern",
    ])
  })
})

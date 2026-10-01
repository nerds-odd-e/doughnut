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

  it("retains occupied list-capable base keys", () => {
    const defaults = richModeKeyDropdownPresetKeys(false)
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("url", "https://x"),
      ])
    ).toEqual(defaults)
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("example of", "[[A]]"),
        propertyRowWithScalar("example of 2", "[[B]]"),
      ])
    ).toEqual(defaults)
  })

  it("omits occupied aliases, overlaps, and note_level instead of suggesting a suffixed key", () => {
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
    expect(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        propertyRowWithScalar("note_level", "2"),
      ])
    ).toEqual(defaults.filter((k) => k !== "note_level"))
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
      "note_level",
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

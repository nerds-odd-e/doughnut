import { describe, expect, it } from "vitest"
import { joinDictatedSegments } from "@/models/audio/joinDictatedSegments"

describe("joinDictatedSegments", () => {
  it.each([
    [
      "Latin to hiragana",
      "私はPython",
      ["が好きです。"],
      "私はPythonが好きです。",
    ],
    [
      "Latin on both sides",
      "私はPython",
      ["is useful."],
      "私はPython is useful.",
    ],
    ["empty base", "", ["果樹園は古いです。"], "果樹園は古いです。"],
    [
      "whitespace-ending base",
      "鐘は毎時間鳴ります。\n",
      ["果樹園は古いです。"],
      "鐘は毎時間鳴ります。\n果樹園は古いです。",
    ],
  ])("%s", (_label, base, segments, joined) => {
    expect(joinDictatedSegments(base, segments)).toBe(joined)
  })
})

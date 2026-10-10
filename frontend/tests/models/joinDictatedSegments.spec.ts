import { describe, expect, it } from "vitest"
import {
  dictatedInsertion,
  joinDictatedSegments,
} from "@/models/audio/joinDictatedSegments"

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

describe("dictatedInsertion", () => {
  it.each([
    {
      case: "empty before and after",
      before: "",
      segments: ["notes"],
      after: "",
      inserted: "notes",
    },
    {
      case: "Latin before",
      before: "Orchard",
      segments: ["notes"],
      after: "",
      inserted: " notes",
    },
    {
      case: "Latin before and after",
      before: "Orchard",
      segments: ["harvest"],
      after: "notes",
      inserted: " harvest ",
    },
    {
      case: "Japanese before",
      before: "りんご園",
      segments: ["の手入れ"],
      after: "",
      inserted: "の手入れ",
    },
    {
      case: "Japanese after",
      before: "",
      segments: ["りんご園"],
      after: "の手入れ",
      inserted: "りんご園",
    },
    {
      case: "Japanese on both sides",
      before: "りんご",
      segments: ["園"],
      after: "の手入れ",
      inserted: "園",
    },
    {
      case: "whitespace already before",
      before: "Orchard ",
      segments: ["harvest"],
      after: "",
      inserted: "harvest",
    },
    {
      case: "whitespace already after",
      before: "Orchard",
      segments: ["harvest"],
      after: " notes",
      inserted: " harvest",
    },
    {
      case: "several segments",
      before: "Orchard",
      segments: ["harvest", "day"],
      after: "notes",
      inserted: " harvest day ",
    },
  ])("$case", ({ before, segments, after, inserted }) => {
    expect(dictatedInsertion(before, segments, after)).toBe(inserted)
  })
})

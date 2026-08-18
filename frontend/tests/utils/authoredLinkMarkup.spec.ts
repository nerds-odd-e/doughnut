import { describe, expect, it } from "vitest"
import {
  authoredLinkOccurrences,
  splitAuthoredToken,
} from "@/utils/authoredLinkMarkup"

describe("authoredLinkMarkup", () => {
  it("authoredLinkOccurrences lists wiki and path Markdown in document order", () => {
    const source = "See [[Folder/Title|wiki]] and [label](/Folder/Title.md)."
    const occ = authoredLinkOccurrences(source)
    expect(occ.map((o) => o.kind)).toEqual(["wiki", "pathMarkdown"])
    expect(occ.map((o) => o.token)).toEqual([
      "Folder/Title|wiki",
      "[label](/Folder/Title.md)",
    ])
    expect(source.slice(occ[0]!.start, occ[0]!.end)).toBe(
      "[[Folder/Title|wiki]]"
    )
    expect(source.slice(occ[1]!.start, occ[1]!.end)).toBe(
      "[label](/Folder/Title.md)"
    )
  })

  it("authoredLinkOccurrences skips image markdown and note-show hrefs", () => {
    expect(
      authoredLinkOccurrences(
        "![alt](/Folder/Title.md) [stay](/n42) [ok](/Folder/Title)"
      ).map((o) => o.token)
    ).toEqual(["[ok](/Folder/Title)"])
  })

  it("authoredLinkOccurrences skips a bare path that is not a Markdown token", () => {
    expect(authoredLinkOccurrences("source: /folder/File.md")).toEqual([])
  })

  it("splitAuthoredToken reads path Markdown href as target", () => {
    expect(splitAuthoredToken("[label](/Folder/Title.md)")).toEqual({
      target: "/Folder/Title.md",
      display: "label",
    })
  })
})

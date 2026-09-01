import { describe, expect, it } from "vitest"
import {
  authoredLinkOccurrences,
  splitWikiLinkInner,
  wikiLinkFromAuthoredToken,
} from "@/utils/authoredLinkMarkup"

describe("authoredLinkMarkup", () => {
  it("wikiLinkFromAuthoredToken names RESOLVED with destinationNoteId", () => {
    expect(wikiLinkFromAuthoredToken("MyNote", 42)).toEqual({
      authoredLink: "MyNote",
      target: "MyNote",
      displayText: "MyNote",
      resolution: "RESOLVED",
      destinationNoteId: 42,
    })
  })

  it("authoredLinkOccurrences lists wiki links in document order", () => {
    const source = "See [[Folder/Title|wiki]] and [label](/Folder/Title.md)."
    const occ = authoredLinkOccurrences(source)
    expect(occ.map((o) => o.token)).toEqual(["Folder/Title|wiki"])
    expect(source.slice(occ[0]!.start, occ[0]!.end)).toBe(
      "[[Folder/Title|wiki]]"
    )
  })

  it("authoredLinkOccurrences ignores Markdown URLs and images", () => {
    expect(
      authoredLinkOccurrences(
        "![alt](/Folder/Title.md) [stay](/n42) [ok](/Folder/Title) [[Wiki]]"
      ).map((o) => o.token)
    ).toEqual(["Wiki"])
  })

  it("splitWikiLinkInner reads wiki pipe target and display", () => {
    expect(splitWikiLinkInner("Target|label")).toEqual({
      target: "Target",
      display: "label",
    })
  })
})

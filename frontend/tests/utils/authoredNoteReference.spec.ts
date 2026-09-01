import { describe, expect, it } from "vitest"
import {
  authoredNoteReferencesInOccurrenceOrder,
  wikiPortablePathTargetFromInner,
  wikiPortablePathTargetsInOccurrenceOrder,
} from "@/utils/authoredNoteReference"

describe("authoredNoteReference", () => {
  it("emits only wiki Portable-path targets from mixed markdown", () => {
    expect(
      authoredNoteReferencesInOccurrenceOrder(
        "See [[Folder/Title|wiki]] and [label](/n42) plus [path](/Folder/Title.md)."
      )
    ).toEqual([
      {
        kind: "wikiPortablePath",
        authoredLink: "Folder/Title|wiki",
        portablePath: "Folder/Title",
        displayText: "wiki",
      },
    ])
  })

  it("wikiPortablePathTargetsInOccurrenceOrder emits wiki only from mixed markdown", () => {
    expect(
      wikiPortablePathTargetsInOccurrenceOrder("[[Alpha]] [Beta](/n9)")
    ).toEqual([wikiPortablePathTargetFromInner("Alpha")])
  })
})

import { describe, expect, it } from "vitest"
import {
  authoredNoteReferencesInOccurrenceOrder,
  noteIdFromHref,
  noteIdFromRootRelativeHref,
  wikiPortablePathTargetFromInner,
  wikiPortablePathTargetsInOccurrenceOrder,
} from "@/utils/authoredNoteReference"

describe("authoredNoteReference", () => {
  it("emits wiki Portable-path and root-relative note-ID URL targets in order", () => {
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
      {
        kind: "noteIdUrl",
        authoredLink: "[label](/n42)",
        noteId: 42,
        href: "/n42",
        displayText: "label",
      },
    ])
  })

  it("emits absolute URLs on the configured origin and skips foreign origins", () => {
    expect(
      authoredNoteReferencesInOccurrenceOrder(
        "[abs](https://donut.test/n99) [foreign](https://evil.example/n99)",
        "https://donut.test"
      )
    ).toEqual([
      {
        kind: "noteIdUrl",
        authoredLink: "[abs](https://donut.test/n99)",
        noteId: 99,
        href: "https://donut.test/n99",
        displayText: "abs",
      },
    ])
  })

  it("skips retired redirect and property note URLs", () => {
    expect(
      authoredNoteReferencesInOccurrenceOrder(
        "[a](/n/9) [b](/n9/p/topic) [c](/n9?x=1) [ok](/n9)"
      )
    ).toEqual([
      {
        kind: "noteIdUrl",
        authoredLink: "[ok](/n9)",
        noteId: 9,
        href: "/n9",
        displayText: "ok",
      },
    ])
  })

  it("wikiPortablePathTargetsInOccurrenceOrder emits wiki only from mixed markdown", () => {
    expect(
      wikiPortablePathTargetsInOccurrenceOrder("[[Alpha]] [Beta](/n9)")
    ).toEqual([wikiPortablePathTargetFromInner("Alpha")])
  })

  it("noteIdFromRootRelativeHref accepts only canonical compact paths", () => {
    expect(noteIdFromRootRelativeHref("/n1234")).toBe(1234)
    expect(noteIdFromRootRelativeHref("/n/1234")).toBeUndefined()
    expect(noteIdFromRootRelativeHref("/n1234/p/x")).toBeUndefined()
  })

  it("noteIdFromHref accepts absolute URLs only on the given origin", () => {
    expect(noteIdFromHref("https://donut.test/n42", "https://donut.test")).toBe(
      42
    )
    expect(
      noteIdFromHref("https://evil.example/n42", "https://donut.test")
    ).toBeUndefined()
    expect(noteIdFromHref("/n42", "https://donut.test")).toBe(42)
  })
})

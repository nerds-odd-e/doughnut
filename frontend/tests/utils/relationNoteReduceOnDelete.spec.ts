import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { qualifyRelationNoteForReduceOnDelete } from "@/utils/relationNoteReduceOnDelete"
import { wikiTitleFromInnerAndNoteId } from "@/utils/wikiPropertyValueField"
import { relationshipNoteContent } from "@tests/notes/relationshipNoteTestContent"

describe("qualifyRelationNoteForReduceOnDelete", () => {
  const moonId = 101
  const earthId = 102

  it("qualifies when type, relation, resolvable source, and target are present", () => {
    const realm = makeMe.aNoteRealm
      .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
      .wikiTitles([
        wikiTitleFromInnerAndNoteId("Moon", moonId),
        wikiTitleFromInnerAndNoteId("Earth", earthId),
      ])
      .please()

    expect(qualifyRelationNoteForReduceOnDelete(realm)).toEqual({
      sourcePropertyKey: "a part of",
      sourceNoteId: moonId,
    })
  })

  it("returns undefined for a normal note", () => {
    expect(
      qualifyRelationNoteForReduceOnDelete(
        makeMe.aNoteRealm.content("Just a note").please()
      )
    ).toBeUndefined()
  })

  it("returns undefined when source wiki link does not resolve", () => {
    const realm = makeMe.aNoteRealm
      .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
      .wikiTitles([wikiTitleFromInnerAndNoteId("Earth", earthId)])
      .please()

    expect(qualifyRelationNoteForReduceOnDelete(realm)).toBeUndefined()
  })

  it("returns undefined when relation label cannot be derived", () => {
    const realm = makeMe.aNoteRealm
      .content(`---
type: relationship
source: "[[Moon]]"
target: "[[Earth]]"
---
`)
      .wikiTitles([wikiTitleFromInnerAndNoteId("Moon", moonId)])
      .please()

    expect(qualifyRelationNoteForReduceOnDelete(realm)).toBeUndefined()
  })
})

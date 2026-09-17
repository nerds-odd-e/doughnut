import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { qualifyRelationNoteForReduceOnTrash } from "@/utils/relationNoteReduceOnTrash"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { relationshipNoteContent } from "@tests/notes/relationshipNoteTestContent"

describe("qualifyRelationNoteForReduceOnTrash", () => {
  const moonId = 101
  const earthId = 102

  it("qualifies when type, relation, resolvable source, and target are present", () => {
    const realm = makeMe.aNoteRealm
      .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
      .wikiLinks([
        wikiLinkFromAuthoredToken("Moon", moonId),
        wikiLinkFromAuthoredToken("Earth", earthId),
      ])
      .please()

    expect(qualifyRelationNoteForReduceOnTrash(realm)).toEqual({
      sourcePropertyKey: "a part of",
      sourceNoteId: moonId,
    })
  })

  it("returns undefined for a normal note", () => {
    expect(
      qualifyRelationNoteForReduceOnTrash(
        makeMe.aNoteRealm.content("Just a note").please()
      )
    ).toBeUndefined()
  })

  it("returns undefined when source wiki link does not resolve", () => {
    const realm = makeMe.aNoteRealm
      .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
      .wikiLinks([wikiLinkFromAuthoredToken("Earth", earthId)])
      .please()

    expect(qualifyRelationNoteForReduceOnTrash(realm)).toBeUndefined()
  })

  it("returns undefined when relation label cannot be derived", () => {
    const realm = makeMe.aNoteRealm
      .content(`---
type: Relationship
source: "[[Moon]]"
target: "[[Earth]]"
---
`)
      .wikiLinks([wikiLinkFromAuthoredToken("Moon", moonId)])
      .please()

    expect(qualifyRelationNoteForReduceOnTrash(realm)).toBeUndefined()
  })
})

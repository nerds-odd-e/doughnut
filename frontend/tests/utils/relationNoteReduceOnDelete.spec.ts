import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { qualifyRelationNoteForReduceOnDelete } from "@/utils/relationNoteReduceOnDelete"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { relationshipNoteContent } from "@tests/notes/relationshipNoteTestContent"

describe("qualifyRelationNoteForReduceOnDelete", () => {
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

    expect(qualifyRelationNoteForReduceOnDelete(realm)).toEqual({
      sourcePropertyKey: "a part of",
      sourceNoteId: moonId,
    })
  })

  it("qualifies when source is a resolvable path Markdown token", () => {
    const realm = makeMe.aNoteRealm
      .content(
        relationshipNoteContent("a-part-of", "[Moon](/Moon.md)", "[[Earth]]")
      )
      .wikiLinks([wikiLinkFromAuthoredToken("[Moon](/Moon.md)", moonId)])
      .please()

    expect(qualifyRelationNoteForReduceOnDelete(realm)?.sourceNoteId).toBe(
      moonId
    )
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
      .wikiLinks([wikiLinkFromAuthoredToken("Earth", earthId)])
      .please()

    expect(qualifyRelationNoteForReduceOnDelete(realm)).toBeUndefined()
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

    expect(qualifyRelationNoteForReduceOnDelete(realm)).toBeUndefined()
  })
})

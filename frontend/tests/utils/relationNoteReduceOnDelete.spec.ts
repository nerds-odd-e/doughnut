import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { isRelationshipNote } from "@/utils/relationNoteReduceOnDelete"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { relationshipNoteContent } from "@tests/notes/relationshipNoteTestContent"

describe("isRelationshipNote", () => {
  const moonId = 101
  const earthId = 102

  it("is true for a relationship-typed note, regardless of resolvable source/target", () => {
    const realm = makeMe.aNoteRealm
      .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
      .wikiLinks([
        wikiLinkFromAuthoredToken("Moon", moonId),
        wikiLinkFromAuthoredToken("Earth", earthId),
      ])
      .please()

    expect(isRelationshipNote(realm)).toBe(true)
  })

  it("returns false for a normal note", () => {
    expect(
      isRelationshipNote(makeMe.aNoteRealm.content("Just a note").please())
    ).toBe(false)
  })

  it("returns false when the note realm has no content", () => {
    expect(isRelationshipNote(undefined)).toBe(false)
  })
})

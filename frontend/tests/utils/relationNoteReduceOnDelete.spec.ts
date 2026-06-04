import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { qualifyRelationNoteForReduceOnDelete } from "@/utils/relationNoteReduceOnDelete"
import { wikiTitleFromInnerAndNoteId } from "@/utils/wikiPropertyValueField"

function relationshipNoteContent(
  relationKebab: string,
  sourceLink: string,
  targetLink: string
): string {
  return `---
type: relationship
relation: ${relationKebab}
source: "${sourceLink.replace(/"/g, '\\"')}"
target: "${targetLink.replace(/"/g, '\\"')}"
---
`
}

describe("qualifyRelationNoteForReduceOnDelete", () => {
  const moonId = 101
  const earthId = 102

  it("qualifies when type, relation, resolvable source, and target are present", () => {
    const realm = {
      ...makeMe.aNoteRealm
        .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
        .please(),
      wikiTitles: [
        wikiTitleFromInnerAndNoteId("Moon", moonId),
        wikiTitleFromInnerAndNoteId("Earth", earthId),
      ],
    }

    expect(qualifyRelationNoteForReduceOnDelete(realm)).toEqual({
      sourcePropertyKey: "a part of",
    })
  })

  it("returns undefined for a normal note", () => {
    const realm = makeMe.aNoteRealm.content("Just a note").please()
    expect(qualifyRelationNoteForReduceOnDelete(realm)).toBeUndefined()
  })

  it("returns undefined when source wiki link does not resolve", () => {
    const realm = {
      ...makeMe.aNoteRealm
        .content(relationshipNoteContent("a-part-of", "[[Moon]]", "[[Earth]]"))
        .please(),
      wikiTitles: [wikiTitleFromInnerAndNoteId("Earth", earthId)],
    }

    expect(qualifyRelationNoteForReduceOnDelete(realm)).toBeUndefined()
  })

  it("returns undefined when relation label cannot be derived", () => {
    const realm = {
      ...makeMe.aNoteRealm
        .content(`---
type: relationship
source: "[[Moon]]"
target: "[[Earth]]"
---
`)
        .please(),
      wikiTitles: [wikiTitleFromInnerAndNoteId("Moon", moonId)],
    }

    expect(qualifyRelationNoteForReduceOnDelete(realm)).toBeUndefined()
  })
})

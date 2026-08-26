import type { NoteRealm } from "@generated/donut-backend-api"
import { relationTypeLabelFromNoteContent } from "@/models/relationTypeOptions"
import {
  authoredLinkOccurrences,
  noteIdForAuthoredToken,
} from "@/utils/authoredLinkMarkup"
import {
  frontmatterScalar,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { wikiTitleNoteIdLookup } from "@/utils/wikiLinkMarkup"

const RELATIONSHIP_NOTE_TYPE = "relationship"

export type RelationNoteReduceOnDeleteQualification = {
  sourcePropertyKey: string
  sourceNoteId: number
}

/** Whether delete may offer reducing this relation note to a source property. */
export function qualifyRelationNoteForReduceOnDelete(
  noteRealm: NoteRealm | undefined
): RelationNoteReduceOnDeleteQualification | undefined {
  const content = noteRealm?.note.content
  if (!content) return
  const parsed = parseNoteContentMarkdown(content)
  if (!parsed.ok) return
  const noteType = frontmatterScalar(parsed.properties, "type")
  if (noteType?.toLowerCase() !== RELATIONSHIP_NOTE_TYPE) return

  const sourcePropertyKey = relationTypeLabelFromNoteContent(content)
  if (!sourcePropertyKey) return

  const sourceScalar = frontmatterScalar(parsed.properties, "source")
  const targetScalar = frontmatterScalar(parsed.properties, "target")
  if (!sourceScalar || !targetScalar) return

  const sourceToken = authoredLinkOccurrences(sourceScalar)[0]?.token
  if (!sourceToken) return

  const sourceNoteId = noteIdForAuthoredToken(
    sourceToken,
    wikiTitleNoteIdLookup(noteRealm.wikiTitles ?? [])
  )
  if (sourceNoteId === undefined) return

  return { sourcePropertyKey, sourceNoteId }
}

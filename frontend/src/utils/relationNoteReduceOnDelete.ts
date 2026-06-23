import type { NoteRealm, WikiTitle } from "@generated/doughnut-backend-api"
import { relationTypeLabelFromNoteContent } from "@/models/relationTypeOptions"
import {
  frontmatterScalar,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import {
  splitWikiLinkInner,
  wikiTitleParts,
} from "@/utils/wikiPropertyValueField"

const RELATIONSHIP_NOTE_TYPE = "relationship"

export type RelationNoteReduceOnDeleteQualification = {
  sourcePropertyKey: string
  sourceNoteId: number
}

function firstWikiLinkInnerInScalar(scalar: string): string | undefined {
  const m = /\[\[([^\[\]\r\n]*)\]\]/.exec(scalar)
  if (!m) return
  const inner = m[1]?.trim()
  return inner === "" ? undefined : inner
}

function noteIdForWikiLinkInner(
  inner: string,
  wikiTitles: WikiTitle[]
): number | undefined {
  const map = new Map<string, number>()
  for (const w of wikiTitles) {
    map.set(w.linkText.trim(), w.noteId)
    map.set(wikiTitleParts(w).target.trim(), w.noteId)
  }
  const { target } = splitWikiLinkInner(inner)
  return map.get(inner) ?? map.get(target.trim())
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

  const sourceInner = firstWikiLinkInnerInScalar(sourceScalar)
  if (!sourceInner) return

  const wikiTitles = noteRealm.wikiTitles ?? []
  const sourceNoteId = noteIdForWikiLinkInner(sourceInner, wikiTitles)
  if (sourceNoteId === undefined) return

  return { sourcePropertyKey, sourceNoteId }
}

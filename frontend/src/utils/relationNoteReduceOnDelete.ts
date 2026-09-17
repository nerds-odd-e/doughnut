import type { NoteRealm } from "@generated/donut-backend-api"
import {
  frontmatterScalar,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"

const RELATIONSHIP_NOTE_TYPE = "relationship"

/**
 * Whether delete may offer reducing this relation note to a source property.
 * The backend derives the property key and resolves+authorizes the source
 * itself, so this only gates on note type.
 */
export function isRelationshipNote(noteRealm: NoteRealm | undefined): boolean {
  const content = noteRealm?.note.content
  if (!content) return false
  const parsed = parseNoteContentMarkdown(content)
  if (!parsed.ok) return false
  const noteType = frontmatterScalar(parsed.properties, "type")
  return noteType?.toLowerCase() === RELATIONSHIP_NOTE_TYPE
}

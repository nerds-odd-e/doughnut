import {
  parseNoteContentMarkdown,
  sortedPropertyRowsFromNoteProperties,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import { migrateLegacyAliasWikiLinksToOverlaps } from "@/utils/migrateLegacyAliasWikiLinksToOverlaps"

/** Property rows for rich frontmatter edit, including legacy alias→overlap migration. */
export function richFrontmatterPropertyRowsFromMarkdown(
  contentMarkdown: string
): PropertyRow[] {
  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok) return []
  const migrated = migrateLegacyAliasWikiLinksToOverlaps(contentMarkdown)
  if (migrated) {
    const m = parseNoteContentMarkdown(migrated)
    if (m.ok) return sortedPropertyRowsFromNoteProperties(m.properties)
  }
  return sortedPropertyRowsFromNoteProperties(parsed.properties)
}

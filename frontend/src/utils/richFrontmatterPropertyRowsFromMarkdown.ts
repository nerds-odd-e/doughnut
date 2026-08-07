import {
  parseNoteContentMarkdown,
  sortedPropertyRowsFromNoteProperties,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"

/** Property rows for rich frontmatter edit from note content markdown. */
export function richFrontmatterPropertyRowsFromMarkdown(
  contentMarkdown: string
): PropertyRow[] {
  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok) return []
  return sortedPropertyRowsFromNoteProperties(parsed.properties)
}

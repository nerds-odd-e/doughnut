import { appendItemToFrontmatterStringList } from "./frontmatterStringList"

/**
 * Returns updated note markdown with `alias` in frontmatter `aliases`, or null when
 * content is unchanged, unparseable, or aliases is present but not a YAML list.
 */
export function appendAliasToNoteContent(
  contentMarkdown: string,
  alias: string
): string | null {
  return appendItemToFrontmatterStringList(contentMarkdown, "aliases", alias)
}

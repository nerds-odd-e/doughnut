import {
  appendItemToFrontmatterStringList,
  frontmatterStringListHasItem,
} from "./frontmatterStringList"

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

/** True when the note's frontmatter `aliases` list already holds `alias`. */
export function noteContentHasAlias(
  contentMarkdown: string,
  alias: string
): boolean {
  return frontmatterStringListHasItem(contentMarkdown, "aliases", alias)
}

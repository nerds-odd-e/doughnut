/**
 * Note titles reserved for notebook/folder readme content (not ordinary notes).
 * Matches `readme` and `readme.md` case-insensitively — keep in sync with backend
 * `ReservedReadmeTitles`.
 */
export const RESERVED_README_TITLE_MESSAGE =
  "'readme' and 'readme.md' are reserved for notebook and folder readme content."

export function isReservedReadmeNoteTitle(
  title: string | null | undefined
): boolean {
  if (title == null) return false
  const trimmed = title.trim().toLowerCase()
  return trimmed === "readme" || trimmed === "readme.md"
}

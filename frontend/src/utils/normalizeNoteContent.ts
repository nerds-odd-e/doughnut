/**
 * Normalizes note content by removing trailing empty lines and meaningless HTML tags.
 * Removes trailing consecutive empty lines, <br>, and <p><br></p> tags.
 */
export function normalizeNoteContent(value: string): string {
  if (!value) return ""

  let normalized = value.trim()

  normalized = normalized.replace(/(\s*<p><br><\/p>\s*)+$/, "")

  normalized = normalized.replace(/(\s*<br>\s*)+$/gi, "")

  normalized = normalized.replace(/\n\s*$/g, "")
  normalized = normalized.replace(/\n+$/, "")

  return normalized.trim()
}

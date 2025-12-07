/**
 * Normalizes note details by removing trailing empty lines and meaningless HTML tags.
 * Removes trailing consecutive empty lines, <br>, and <p><br></p> tags.
 * @param value - The raw note details value
 * @returns The normalized value with trailing empty content removed
 */
export function normalizeNoteDetails(value: string): string {
  if (!value) return ""

  let normalized = value.trim()

  // Remove trailing <p><br></p> tags (with optional whitespace and newlines)
  normalized = normalized.replace(/(\s*<p><br><\/p>\s*)+$/, "")

  // Remove trailing <br> tags (with optional whitespace and newlines)
  normalized = normalized.replace(/(\s*<br>\s*)+$/gi, "")

  // Remove trailing empty lines (lines with only whitespace)
  normalized = normalized.replace(/\n\s*$/g, "")
  normalized = normalized.replace(/\n+$/, "")

  return normalized.trim()
}

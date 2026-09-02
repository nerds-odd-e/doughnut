/** Whether a note's content is non-empty once whitespace is trimmed. */
export function hasNoteContent(content: string | null | undefined): boolean {
  return !!(content ?? "").trim()
}

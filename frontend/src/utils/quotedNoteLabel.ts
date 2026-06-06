export function quotedNoteLabel(
  title: string | undefined | null,
  noteId: number
): string {
  const trimmed = title?.trim() ?? ""
  return trimmed ? `"${trimmed}"` : `note id: ${noteId}`
}

import type { Note, NoteTopology } from 'doughnut-api'

/**
 * Titles from root note → current note (web Breadcrumb with includingSelf).
 * Used for recall scrollback and card payloads whenever the API attached `note`.
 */
export function noteBreadcrumbTrailTitles(
  note: Note | undefined
): readonly string[] {
  return titlesAlongNoteTopology(note?.noteTopology)
}

function titlesAlongNoteTopology(
  root: NoteTopology | undefined
): readonly string[] {
  if (root === undefined) {
    return ['Note']
  }
  const chain: NoteTopology[] = []
  let current: NoteTopology | undefined = root
  while (current !== undefined) {
    chain.push(current)
    current = current.parentOrSubjectNoteTopology
  }
  chain.reverse()
  return chain.map((n) => {
    const t = n.title?.trim()
    return t !== undefined && t.length > 0 ? t : 'Note'
  })
}

/** Prefer note body from an answered prompt; else text cached when the session loaded the card. */
export function noteDetailsMarkdownOrFallback(
  note: Note | undefined,
  fallbackDetailsMarkdown: string
): string {
  const d = note?.details?.trim()
  if (d !== undefined && d.length > 0) {
    return d
  }
  return fallbackDetailsMarkdown.trim()
}

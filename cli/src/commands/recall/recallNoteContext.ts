import type { Note, NoteTopology } from 'doughnut-api'

/** Minimal shape for folder trail segments (matches API `FolderTrailSegment`). */
type FolderTrailLike = { name?: string }

/**
 * Titles for recall scrollback: notebook, folder path (outer→inner), then note title.
 * Pass `ancestorFolders` from `MemoryTracker` / `RecallPrompt` when available.
 */
export function noteBreadcrumbTrailTitles(
  note: Note | undefined,
  ancestorFolders?: readonly FolderTrailLike[] | undefined
): readonly string[] {
  if (note === undefined) {
    return ['Note']
  }
  const topo = note.noteTopology
  return titlesFromTopologyAndFolders(topo, ancestorFolders)
}

function titlesFromTopologyAndFolders(
  topo: NoteTopology | undefined,
  ancestorFolders: readonly FolderTrailLike[] | undefined
): readonly string[] {
  if (topo === undefined) {
    return ['Note']
  }
  const parts: string[] = []
  const notebook = topo.notebookName?.trim()
  if (notebook !== undefined && notebook.length > 0) {
    parts.push(notebook)
  }
  for (const seg of ancestorFolders ?? []) {
    const n = seg.name?.trim()
    if (n !== undefined && n.length > 0) {
      parts.push(n)
    }
  }
  const title = topo.title?.trim()
  parts.push(title !== undefined && title.length > 0 ? title : 'Note')
  return parts
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

import {
  authoredLinkOccurrences,
  splitWikiLinkInner,
} from "@/utils/authoredLinkMarkup"
import { MARKDOWN_LINK, noteIdFromRootRelativeHref } from "@/utils/noteIdUrl"

/**
 * Authored semantic note reference in note content (ADR 0001 Wiki link). Distinguishes wiki
 * Portable-path spelling from a Markdown note-ID URL target.
 */
export type AuthoredNoteReference = WikiPortablePathTarget | NoteIdUrlTarget

export type WikiPortablePathTarget = {
  kind: "wikiPortablePath"
  /** Inner text between `[[` and `]]`. */
  authoredLink: string
  portablePath: string
  displayText: string
}

export type NoteIdUrlTarget = {
  kind: "noteIdUrl"
  /** Full Markdown link spelling stored as the authored link. */
  authoredLink: string
  noteId: number
  href: string
  displayText: string
}

export function wikiPortablePathTargetFromInner(
  authoredInner: string
): WikiPortablePathTarget {
  const { target, display } = splitWikiLinkInner(authoredInner)
  return {
    kind: "wikiPortablePath",
    authoredLink: authoredInner,
    portablePath: target,
    displayText: display,
  }
}

export { noteIdFromRootRelativeHref } from "@/utils/noteIdUrl"

type Hit = { start: number; ref: AuthoredNoteReference }

/**
 * Authored note references in document order: wiki Portable-path targets and recognized
 * root-relative note-ID URLs.
 */
export function authoredNoteReferencesInOccurrenceOrder(
  markdown: string
): AuthoredNoteReference[] {
  if (markdown.length === 0) return []
  const hits: Hit[] = []
  for (const occ of authoredLinkOccurrences(markdown)) {
    hits.push({
      start: occ.start,
      ref: wikiPortablePathTargetFromInner(occ.token),
    })
  }
  for (const m of markdown.matchAll(MARKDOWN_LINK)) {
    const href = m[2]!
    const noteId = noteIdFromRootRelativeHref(href)
    if (noteId === undefined) continue
    hits.push({
      start: m.index,
      ref: {
        kind: "noteIdUrl",
        authoredLink: m[0],
        noteId,
        href,
        displayText: m[1]!,
      },
    })
  }
  hits.sort((a, b) => a.start - b.start)
  return hits.map((h) => h.ref)
}

/** Wiki Portable-path targets only (skips note-ID URL kind when present). */
export function wikiPortablePathTargetsInOccurrenceOrder(
  markdown: string
): WikiPortablePathTarget[] {
  return authoredNoteReferencesInOccurrenceOrder(markdown).flatMap((ref) =>
    ref.kind === "wikiPortablePath" ? [ref] : []
  )
}

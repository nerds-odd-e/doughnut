import {
  authoredLinkOccurrences,
  splitWikiLinkInner,
} from "@/utils/authoredLinkMarkup"

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

/**
 * Authored note references in document order. Currently emits only wiki Portable-path
 * targets; note-ID URL recognition is not wired yet.
 */
export function authoredNoteReferencesInOccurrenceOrder(
  markdown: string
): AuthoredNoteReference[] {
  return authoredLinkOccurrences(markdown).map((occ) =>
    wikiPortablePathTargetFromInner(occ.token)
  )
}

/** Wiki Portable-path targets only (skips note-ID URL kind when present). */
export function wikiPortablePathTargetsInOccurrenceOrder(
  markdown: string
): WikiPortablePathTarget[] {
  return authoredNoteReferencesInOccurrenceOrder(markdown).flatMap((ref) =>
    ref.kind === "wikiPortablePath" ? [ref] : []
  )
}

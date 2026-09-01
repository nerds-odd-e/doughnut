/**
 * Canonical compact Donut note-show URL recognition (ADR 0005). Root-relative `/n<ID>` only —
 * not retired `/n/<ID>` redirects, property paths, or query/hash variants.
 */

const ROOT_RELATIVE_NOTE_HREF = /^\/n(\d+)$/

/** Note id when href is exactly the canonical compact path `/n<digits>`. */
export function noteIdFromRootRelativeHref(href: string): number | undefined {
  const m = ROOT_RELATIVE_NOTE_HREF.exec(href)
  if (!m) return undefined
  return Number(m[1])
}

/** Markdown `[display](href)`; skips the second `[` of wiki `[[…]]`. */
export const MARKDOWN_LINK = /(?<!\[)\[([^\]]*)]\(([^)]+)\)/g

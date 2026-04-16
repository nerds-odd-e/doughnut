/**
 * Minimal block shape for EPUB current-location mapping (OpenAPI `BookBlockFull` is compatible).
 */
export type BookBlockEpubLocationRow = {
  readonly id: number
  readonly epubStartHref?: string
}

function splitEpubHref(href: string): {
  path: string
  fragment: string | null
} {
  const i = href.indexOf("#")
  if (i === -1) {
    return { path: href, fragment: null }
  }
  const fragment = href.slice(i + 1)
  return {
    path: href.slice(0, i),
    fragment: fragment.length === 0 ? null : fragment,
  }
}

/**
 * Maps an epub.js spine location (`location.start.href`, optionally with `#fragment`)
 * to the owning `BookBlock.id`. `blocks` must be depth-first preorder (same order as the API).
 *
 * Rule: among blocks with a non-null `epubStartHref` whose path (before `#`) equals the
 * relocated path, take the last in preorder. If the relocated href includes a fragment,
 * prefer the last in preorder whose stored fragment matches; if none match, fall back to
 * the last path-only match.
 */
export function currentBlockIdFromEpubLocation(
  blocks: readonly BookBlockEpubLocationRow[],
  href: string
): number | null {
  if (typeof href !== "string" || href.length === 0) {
    return null
  }
  const trimmed = href.trim()
  const { path: relPath, fragment: relFragment } = splitEpubHref(trimmed)
  if (relPath.length === 0) {
    return null
  }

  let lastPathMatchId: number | null = null
  let lastFragmentMatchId: number | null = null

  for (const block of blocks) {
    const start = block.epubStartHref
    if (start === undefined || start === null || start === "") {
      continue
    }
    const { path: blockPath, fragment: blockFragment } = splitEpubHref(
      start.trim()
    )
    if (blockPath !== relPath) {
      continue
    }
    lastPathMatchId = block.id
    if (
      relFragment !== null &&
      blockFragment !== null &&
      blockFragment === relFragment
    ) {
      lastFragmentMatchId = block.id
    }
  }

  if (relFragment !== null) {
    return lastFragmentMatchId ?? lastPathMatchId
  }
  return lastPathMatchId
}

/**
 * Minimal block shape for EPUB current-location mapping (OpenAPI `BookBlockFull` is compatible).
 */
export type BookBlockEpubLocationRow = {
  readonly id: number
  readonly epubStartHref?: string
}

/**
 * epub.js spine `href` is often manifest-relative (e.g. `chapter1.xhtml`) while our API stores
 * package-root paths (e.g. `OEBPS/chapter1.xhtml`).
 */
function epubSpinePathMatches(
  storedPath: string,
  relocatedPath: string
): boolean {
  const a = storedPath.replace(/^\/+/, "").trim()
  const b = relocatedPath.replace(/^\/+/, "").trim()
  if (a === b) {
    return true
  }
  return a.endsWith(`/${b}`) || b.endsWith(`/${a}`)
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
  const { path: relPath, fragment: relFragment } = splitEpubHref(href.trim())
  if (relPath.length === 0) {
    return null
  }

  let lastPathMatchId: number | null = null
  let lastFragmentMatchId: number | null = null

  for (const block of blocks) {
    if (!block.epubStartHref) {
      continue
    }
    const { path: blockPath, fragment: blockFragment } = splitEpubHref(
      block.epubStartHref.trim()
    )
    if (!epubSpinePathMatches(blockPath, relPath)) {
      continue
    }
    lastPathMatchId = block.id
    if (relFragment !== null && blockFragment === relFragment) {
      lastFragmentMatchId = block.id
    }
  }

  if (relFragment !== null) {
    return lastFragmentMatchId ?? lastPathMatchId
  }
  return lastPathMatchId
}

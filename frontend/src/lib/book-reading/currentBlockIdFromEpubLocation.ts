import { blockStartEpubDisplayHref } from "@/lib/book-reading/asEpubLocator"
import {
  epubSpinePathMatches,
  splitEpubHref,
} from "@/lib/book-reading/epubHrefMatch"
import type { BookBlockFull } from "@generated/doughnut-backend-api"

export type BookBlockEpubLocationRow = Pick<
  BookBlockFull,
  "id" | "contentLocators"
>

/**
 * Maps an epub.js spine location (`location.start.href`, optionally with `#fragment`)
 * to the owning `BookBlock.id`. `blocks` must be depth-first preorder (same order as the API).
 *
 * Rule: among blocks with a resolvable EPUB start locator whose path (before `#`) equals the
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
    const displayHref = blockStartEpubDisplayHref(block)
    if (!displayHref) {
      continue
    }
    const { path: blockPath, fragment: blockFragment } = splitEpubHref(
      displayHref.trim()
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

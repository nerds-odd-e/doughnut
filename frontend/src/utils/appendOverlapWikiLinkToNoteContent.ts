import type { WikiLink } from "@generated/donut-backend-api"
import { authoredWikiLinkTokenForInsert } from "./wikiLinkAuthoring"
import { appendItemToFrontmatterStringList } from "./frontmatterStringList"
import { noteContentDeclaresOverlapToDestination } from "./overlapWikiLinkTokens"

/** Appends a backend-authored Portable-path wiki-link token to the note's `overlaps` frontmatter list. */
export async function appendOverlapWikiLinkToNoteContent(
  contentMarkdown: string,
  sourceNoteId: number,
  destinationNoteId: number,
  wikiLinks: readonly WikiLink[]
): Promise<string | null> {
  if (
    noteContentDeclaresOverlapToDestination(
      contentMarkdown,
      wikiLinks,
      destinationNoteId
    )
  ) {
    return null
  }
  const token = await authoredWikiLinkTokenForInsert(
    sourceNoteId,
    destinationNoteId
  )
  if (token === undefined) return null
  return appendItemToFrontmatterStringList(contentMarkdown, "overlaps", token)
}

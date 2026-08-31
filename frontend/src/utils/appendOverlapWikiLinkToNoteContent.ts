import { authoredWikiLinkTokenForInsert } from "./sameNotebookWikiLinkAuthoring"
import { appendItemToFrontmatterStringList } from "./frontmatterStringList"

/** Appends a backend-authored Portable-path wiki-link token to the note's `overlaps` frontmatter list. */
export async function appendOverlapWikiLinkToNoteContent(
  contentMarkdown: string,
  sourceNoteId: number,
  destinationNoteId: number
): Promise<string | null> {
  const token = await authoredWikiLinkTokenForInsert(
    sourceNoteId,
    destinationNoteId
  )
  if (token === undefined) return null
  return appendItemToFrontmatterStringList(contentMarkdown, "overlaps", token)
}

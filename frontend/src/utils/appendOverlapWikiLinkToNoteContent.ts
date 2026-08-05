import { buildWikiLinkText } from "./buildWikiLinkText"
import { appendAliasToNoteContent } from "./wikidataTitleActions"

export function appendOverlapWikiLinkToNoteContent(
  contentMarkdown: string,
  target: {
    noteTopology: { title: string }
    notebookId: number
    notebookName?: string
  },
  source: { notebookId?: number }
): string | null {
  const token = buildWikiLinkText(target, { notebookId: source.notebookId })
  return appendAliasToNoteContent(contentMarkdown, token)
}

export function buildWikiLinkText(
  target: {
    noteTopology: { title?: string }
    notebookId: number
    notebookName?: string
  },
  source: { notebookId?: number }
): string {
  const title = target.noteTopology.title ?? ""
  if (
    source.notebookId !== undefined &&
    target.notebookId !== source.notebookId &&
    target.notebookName
  ) {
    return `[[${target.notebookName}:${title}]]`
  }
  return `[[${title}]]`
}

export function buildWikiLinkText(
  target: {
    noteTopology: { title: string }
    notebookId: number
    notebookName?: string
  },
  source: { notebookId?: number; displayText?: string }
): string {
  const title = target.noteTopology.title
  const useNotebookPrefix =
    source.notebookId !== undefined &&
    target.notebookId !== source.notebookId &&
    Boolean(target.notebookName)

  const defaultInner = useNotebookPrefix
    ? `${target.notebookName}:${title}`
    : title

  const trimmedDisplay = source.displayText?.trim() ?? ""
  const inner =
    trimmedDisplay.length > 0 &&
    defaultInner.length > 0 &&
    trimmedDisplay !== defaultInner
      ? `${defaultInner}|${trimmedDisplay}`
      : defaultInner

  return `[[${inner}]]`
}

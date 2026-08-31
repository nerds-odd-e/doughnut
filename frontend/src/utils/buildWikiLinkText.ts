import {
  encodeWikiLinkPropertyKey,
  formatPortablePath,
} from "@/utils/portablePath"

export type WikiLinkNoteIdentity = {
  noteTopology: { title: string }
  notebookId: number
  notebookName?: string
}

function defaultWikiNoteTarget(
  target: WikiLinkNoteIdentity,
  sourceNotebookId: number | undefined
): string {
  const title = target.noteTopology.title
  const useNotebookPrefix =
    sourceNotebookId !== undefined &&
    target.notebookId !== sourceNotebookId &&
    Boolean(target.notebookName)
  return useNotebookPrefix ? `${target.notebookName}:${title}` : title
}

function wikiLinkFromDefaultInner(
  defaultInner: string,
  displayText: string | undefined
): string {
  const trimmedDisplay = displayText?.trim() ?? ""
  const inner =
    trimmedDisplay.length > 0 &&
    defaultInner.length > 0 &&
    trimmedDisplay !== defaultInner
      ? `${defaultInner}|${trimmedDisplay}`
      : defaultInner
  return `[[${inner}]]`
}

export function buildWikiLinkText(
  target: WikiLinkNoteIdentity,
  source: {
    notebookId?: number
    displayText?: string
    propertyKey?: string
  }
): string {
  const noteTarget = defaultWikiNoteTarget(target, source.notebookId)
  const defaultInner = source.propertyKey
    ? formatPortablePath({
        qualifiedNotePortion: noteTarget,
        encodedPropertyKey: encodeWikiLinkPropertyKey(source.propertyKey),
      })
    : noteTarget
  return wikiLinkFromDefaultInner(defaultInner, source.displayText)
}

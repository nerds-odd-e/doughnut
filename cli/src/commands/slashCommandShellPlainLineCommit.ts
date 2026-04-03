import type { SessionScrollbackAppendApi } from '../sessionScrollback/sessionScrollbackAppendContext.js'

export type PlainLineCommitScrollback = Pick<
  SessionScrollbackAppendApi,
  'appendScrollbackError' | 'appendScrollbackUserMessage'
>

export function commitMainInteractivePlainLine(
  line: string,
  {
    appendScrollbackError,
    appendScrollbackUserMessage,
  }: PlainLineCommitScrollback
): void {
  const assistantText = line.startsWith('/')
    ? 'unsupported command'
    : 'Not supported'
  appendScrollbackUserMessage(line)
  appendScrollbackError(assistantText)
}

export function commitNotebookStagePlainLine(
  line: string,
  {
    appendScrollbackError,
    appendScrollbackUserMessage,
  }: PlainLineCommitScrollback
): void {
  const trimmed = line.trim()
  if (trimmed === '') return
  appendScrollbackUserMessage(trimmed)
  appendScrollbackError('Not supported')
}

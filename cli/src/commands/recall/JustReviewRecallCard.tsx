import type { MutableRefObject } from 'react'
import { Text } from 'ink'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import { YesNoStagePrompt } from '../../YesNoStagePrompt.js'
import type { RecallJustReviewPayload } from './justReviewLoad.js'

const STAGE_LABEL = 'Recalling'

export function JustReviewRecallCard({
  payload,
  onAnswer,
  onCancel,
  inputBlockedRef,
}: {
  readonly payload: RecallJustReviewPayload
  readonly onAnswer: (yes: boolean) => void | Promise<void>
  readonly onCancel: () => void
  readonly inputBlockedRef: MutableRefObject<boolean>
}) {
  const width = resolvedTerminalWidth()
  const detailsRendered = payload.detailsMarkdown
    ? renderMarkdownToTerminal(payload.detailsMarkdown, width)
    : ''
  const detailLines =
    detailsRendered.length > 0 ? detailsRendered.split('\n') : []

  return (
    <YesNoStagePrompt
      key={payload.memoryTrackerId}
      prompt="Yes, I remember?"
      onAnswer={onAnswer}
      onCancel={onCancel}
      inputBlockedRef={inputBlockedRef}
      header={
        <>
          <Text>{STAGE_LABEL}</Text>
          {payload.notebookTitle !== undefined &&
          payload.notebookTitle !== '' ? (
            <Text>{payload.notebookTitle}</Text>
          ) : null}
        </>
      }
      belowBuffer={
        <>
          <Text>{payload.noteTitle}</Text>
          {detailLines.map((line, i) => (
            <Text key={i}>{line.length > 0 ? line : ' '}</Text>
          ))}
        </>
      }
    />
  )
}

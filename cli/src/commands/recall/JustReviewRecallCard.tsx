import { Fragment, useCallback, useState, type MutableRefObject } from 'react'
import { Text } from 'ink'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import { YesNoStagePrompt } from '../../YesNoStagePrompt.js'
import type { RecallJustReviewPayload } from './justReviewLoad.js'

const STAGE_LABEL = 'Recalling'

const LEAVE_RECALL_PROMPT = 'Leave recall?'

export function JustReviewRecallCard({
  payload,
  onAnswer,
  onAbortInFlight,
  onLeaveRecallConfirmed,
  inputBlockedRef,
}: {
  readonly payload: RecallJustReviewPayload
  readonly onAnswer: (yes: boolean) => void | Promise<void>
  readonly onAbortInFlight: () => void
  readonly onLeaveRecallConfirmed: () => void
  readonly inputBlockedRef: MutableRefObject<boolean>
}) {
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)
  const width = resolvedTerminalWidth()
  const detailsRendered = payload.detailsMarkdown
    ? renderMarkdownToTerminal(payload.detailsMarkdown, width)
    : ''
  const detailLines =
    detailsRendered.length > 0 ? detailsRendered.split('\n') : []

  const headerEl = (
    <Fragment>
      <Text>{STAGE_LABEL}</Text>
      {payload.notebookTitle !== undefined && payload.notebookTitle !== '' ? (
        <Text>{payload.notebookTitle}</Text>
      ) : null}
    </Fragment>
  )

  const handleQuestionEsc = useCallback(() => {
    if (inputBlockedRef.current) {
      onAbortInFlight()
      return
    }
    setShowLeaveConfirm(true)
  }, [inputBlockedRef, onAbortInFlight])

  if (showLeaveConfirm) {
    return (
      <YesNoStagePrompt
        prompt={LEAVE_RECALL_PROMPT}
        onAnswer={(yes) => {
          if (yes) {
            onLeaveRecallConfirmed()
            return
          }
          setShowLeaveConfirm(false)
        }}
        onCancel={() => setShowLeaveConfirm(false)}
        inputBlockedRef={inputBlockedRef}
        header={headerEl}
      />
    )
  }

  return (
    <YesNoStagePrompt
      key={payload.memoryTrackerId}
      prompt="Yes, I remember?"
      onAnswer={onAnswer}
      onCancel={handleQuestionEsc}
      inputBlockedRef={inputBlockedRef}
      header={headerEl}
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

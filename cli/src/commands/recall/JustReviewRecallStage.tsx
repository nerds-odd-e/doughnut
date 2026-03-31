import { MemoryTrackerController } from 'doughnut-api'
import { Fragment, useCallback, useState, type MutableRefObject } from 'react'
import { Text } from 'ink'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import { YesNoStagePrompt } from '../../YesNoStagePrompt.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { LeaveRecallConfirmPrompt } from './LeaveRecallConfirmPrompt.js'
import type { RecallJustReviewPayload } from './nextRecallCardLoad.js'
import type { RecallQuestionAnswerOutcome } from './recallQuestionAnswerOutcome.js'

const STAGE_LABEL = 'Recalling'

export function JustReviewRecallStage({
  payload,
  inputBlockedRef,
  activeOperationAbortRef,
  onRecallQuestionAnswered,
  onRecallFatalError,
  onConfirmLeaveRecall,
}: {
  readonly payload: RecallJustReviewPayload
  readonly inputBlockedRef: MutableRefObject<boolean>
  readonly activeOperationAbortRef: MutableRefObject<AbortController | null>
  readonly onRecallQuestionAnswered: (
    outcome: RecallQuestionAnswerOutcome
  ) => void | Promise<void>
  readonly onRecallFatalError: (message: string) => void
  readonly onConfirmLeaveRecall: () => void
}) {
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false)

  const submitJustReview = useCallback(
    async (yesIRemember: boolean) => {
      if (inputBlockedRef.current) return
      const ac = new AbortController()
      activeOperationAbortRef.current = ac
      inputBlockedRef.current = true
      const p = payload
      try {
        try {
          await runDefaultBackendJson(() =>
            MemoryTrackerController.markAsRecalled({
              path: { memoryTracker: p.memoryTrackerId },
              query: { successful: yesIRemember },
              ...doughnutSdkOptions(ac.signal),
            })
          )
        } catch (err: unknown) {
          onRecallFatalError(userVisibleSlashCommandError(err))
          return
        }
        if (ac.signal.aborted) {
          onRecallFatalError(
            userVisibleSlashCommandError(
              new DOMException('Aborted', 'AbortError')
            )
          )
          return
        }
        if (!yesIRemember) {
          await onRecallQuestionAnswered({
            successful: false,
            scrollbackLines: ['Reduced memory index.'],
          })
          return
        }
        await onRecallQuestionAnswered({
          successful: true,
          scrollbackLines: [`Reviewed: ${p.noteTitle}`],
        })
      } finally {
        inputBlockedRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
      }
    },
    [
      activeOperationAbortRef,
      inputBlockedRef,
      onRecallFatalError,
      onRecallQuestionAnswered,
      payload,
    ]
  )

  const abortJustReviewInFlight = useCallback(() => {
    if (inputBlockedRef.current) {
      activeOperationAbortRef.current?.abort()
    }
  }, [activeOperationAbortRef, inputBlockedRef])

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
      abortJustReviewInFlight()
      return
    }
    setShowLeaveConfirm(true)
  }, [abortJustReviewInFlight, inputBlockedRef])

  if (showLeaveConfirm) {
    return (
      <LeaveRecallConfirmPrompt
        onConfirmLeave={onConfirmLeaveRecall}
        onDismiss={() => setShowLeaveConfirm(false)}
        inputBlockedRef={inputBlockedRef}
        header={headerEl}
      />
    )
  }

  return (
    <YesNoStagePrompt
      key={payload.memoryTrackerId}
      prompt="Yes, I remember?"
      onAnswer={submitJustReview}
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

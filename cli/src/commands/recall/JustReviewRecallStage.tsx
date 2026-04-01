import { MemoryTrackerController } from 'doughnut-api'
import {
  Fragment,
  useCallback,
  useState,
  type MutableRefObject,
  type ReactElement,
} from 'react'
import { Text } from 'ink'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { renderMarkdownToTerminal } from '../../markdown.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import { YesNoStagePrompt } from '../../commonUIComponents/YesNoStagePrompt.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { LeaveRecallConfirmPrompt } from './LeaveRecallConfirmPrompt.js'
import type { RecallJustReviewPayload } from './nextRecallCardLoad.js'
import type { RecallQuestionAnswerOutcome } from './recallQuestionAnswerOutcome.js'
import {
  RecallAnsweredBlockShell,
  recallAnsweredBreadcrumbText,
  recallAnsweredMarkdownToDisplayLines,
} from './recallAnsweredInkShared.js'

const STAGE_LABEL = 'Recalling'

function justReviewOutcomeLine(
  outcome: 'remembered' | 'reduced',
  noteTitle: string
): string {
  if (outcome === 'reduced') {
    return 'Reduced memory index.'
  }
  return `Reviewed: ${noteTitle}`
}

function recallAnsweredJustReviewInk(
  payload: RecallJustReviewPayload,
  remembered: boolean
): ReactElement {
  const width = resolvedTerminalWidth()
  const crumb = recallAnsweredBreadcrumbText(payload.breadcrumbTitles)
  const detailLines = recallAnsweredMarkdownToDisplayLines(
    payload.detailsMarkdown,
    width
  )
  const outcome: 'remembered' | 'reduced' = remembered
    ? 'remembered'
    : 'reduced'
  return (
    <RecallAnsweredBlockShell>
      <Text>{crumb}</Text>
      {detailLines.map((line, i) => (
        <Text key={i}>{line.length > 0 ? line : ' '}</Text>
      ))}
      <Text>{justReviewOutcomeLine(outcome, payload.noteTitle)}</Text>
    </RecallAnsweredBlockShell>
  )
}

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
            answeredRows: [recallAnsweredJustReviewInk(p, false)],
          })
          return
        }
        await onRecallQuestionAnswered({
          successful: true,
          answeredRows: [recallAnsweredJustReviewInk(p, true)],
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

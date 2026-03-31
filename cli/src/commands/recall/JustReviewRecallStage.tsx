import { useCallback, type MutableRefObject } from 'react'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { markMemoryTrackerRecalled } from './markMemoryTrackerRecalled.js'
import { JustReviewRecallCard } from './JustReviewRecallCard.js'
import type { RecallJustReviewPayload } from './justReviewLoad.js'
import type { RecallQuestionAnswerOutcome } from './recallQuestionAnswerOutcome.js'

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
  const submitJustReview = useCallback(
    async (yesIRemember: boolean) => {
      if (inputBlockedRef.current) return
      const ac = new AbortController()
      activeOperationAbortRef.current = ac
      inputBlockedRef.current = true
      const p = payload
      try {
        try {
          await markMemoryTrackerRecalled(
            p.memoryTrackerId,
            yesIRemember,
            ac.signal
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
          scrollbackLines: [],
          justReviewSuccessfulRecall: { noteTitle: p.noteTitle },
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

  return (
    <JustReviewRecallCard
      key={payload.memoryTrackerId}
      payload={payload}
      onAnswer={submitJustReview}
      onAbortInFlight={abortJustReviewInFlight}
      onLeaveRecallConfirmed={onConfirmLeaveRecall}
      inputBlockedRef={inputBlockedRef}
    />
  )
}

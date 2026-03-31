import { useCallback, type MutableRefObject } from 'react'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import { useSessionScrollbackAppend } from '../../sessionScrollback/sessionScrollbackAppendContext.js'
import { recallAnsweredLine } from './recallAnsweredScrollback.js'
import { markMemoryTrackerRecalled } from './markMemoryTrackerRecalled.js'
import {
  loadNextRecallCardIfAny,
  type RecallCard,
} from './nextRecallCardLoad.js'
import { JustReviewRecallCard } from './JustReviewRecallCard.js'
import { RECALL_SESSION_STOPPED_LINE } from './leaveRecallSessionCopy.js'
import type { RecallJustReviewPayload } from './justReviewLoad.js'

export function JustReviewRecallStage({
  payload,
  inputBlockedRef,
  activeOperationAbortRef,
  startedWithEmptyTodayRef,
  successfulRecallsRef,
  onSettled,
  setCard,
  setUiMode,
}: {
  readonly payload: RecallJustReviewPayload
  readonly inputBlockedRef: MutableRefObject<boolean>
  readonly activeOperationAbortRef: MutableRefObject<AbortController | null>
  readonly startedWithEmptyTodayRef: MutableRefObject<boolean>
  readonly successfulRecallsRef: MutableRefObject<number>
  readonly onSettled: (message: string) => void
  readonly setCard: (card: RecallCard | null) => void
  readonly setUiMode: (mode: 'card' | 'loadMore') => void
}) {
  const { appendScrollbackItem } = useSessionScrollbackAppend()

  const submitJustReview = useCallback(
    async (successful: boolean) => {
      if (inputBlockedRef.current) return
      const ac = new AbortController()
      activeOperationAbortRef.current = ac
      inputBlockedRef.current = true
      const p = payload
      try {
        await markMemoryTrackerRecalled(
          p.memoryTrackerId,
          successful,
          ac.signal
        )
      } catch (err: unknown) {
        inputBlockedRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled(userVisibleSlashCommandError(err))
        return
      }
      if (ac.signal.aborted) {
        inputBlockedRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled(
          userVisibleSlashCommandError(
            new DOMException('Aborted', 'AbortError')
          )
        )
        return
      }
      if (!successful) {
        inputBlockedRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled('Marked as not recalled.')
        return
      }
      successfulRecallsRef.current += 1
      try {
        const next = await loadNextRecallCardIfAny(0, ac.signal)
        inputBlockedRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        if (ac.signal.aborted) {
          onSettled(
            userVisibleSlashCommandError(
              new DOMException('Aborted', 'AbortError')
            )
          )
          return
        }
        if (next === null) {
          if (
            startedWithEmptyTodayRef.current &&
            successfulRecallsRef.current === 1
          ) {
            appendScrollbackItem(recallAnsweredLine(`Reviewed: ${p.noteTitle}`))
            onSettled('')
          } else {
            setUiMode('loadMore')
          }
        } else {
          appendScrollbackItem(recallAnsweredLine(`Reviewed: ${p.noteTitle}`))
          setCard(next)
        }
      } catch (loadErr: unknown) {
        inputBlockedRef.current = false
        if (activeOperationAbortRef.current === ac) {
          activeOperationAbortRef.current = null
        }
        onSettled(userVisibleSlashCommandError(loadErr))
      }
    },
    [
      activeOperationAbortRef,
      appendScrollbackItem,
      inputBlockedRef,
      onSettled,
      payload,
      setCard,
      setUiMode,
      startedWithEmptyTodayRef,
      successfulRecallsRef,
    ]
  )

  const abortJustReviewInFlight = useCallback(() => {
    if (inputBlockedRef.current) {
      activeOperationAbortRef.current?.abort()
    }
  }, [activeOperationAbortRef, inputBlockedRef])

  const leaveJustReviewSession = useCallback(() => {
    onSettled(RECALL_SESSION_STOPPED_LINE)
  }, [onSettled])

  return (
    <JustReviewRecallCard
      key={payload.memoryTrackerId}
      payload={payload}
      onAnswer={submitJustReview}
      onAbortInFlight={abortJustReviewInFlight}
      onLeaveRecallConfirmed={leaveJustReviewSession}
      inputBlockedRef={inputBlockedRef}
    />
  )
}

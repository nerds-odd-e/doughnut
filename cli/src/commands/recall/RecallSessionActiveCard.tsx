import type { MutableRefObject } from 'react'
import { JustReviewRecallStage } from './JustReviewRecallStage.js'
import { RecallMcqStage } from './RecallMcqStage.js'
import { SpellingRecallStage } from './SpellingRecallStage.js'
import { RecallSessionChrome } from './RecallSessionChrome.js'
import type { RecallCard } from './nextRecallCardLoad.js'
import type { RecallPromptAnswerOutcome } from './recallPromptAnswerOutcome.js'

export function RecallSessionActiveCard({
  card,
  submittingRef,
  activeOperationAbortRef,
  onRecallQuestionAnswered,
  onReplaceCurrentRecallCard,
  onRecallFatalError,
  onConfirmLeaveRecall,
}: {
  readonly card: RecallCard
  readonly submittingRef: MutableRefObject<boolean>
  readonly activeOperationAbortRef: MutableRefObject<AbortController | null>
  readonly onRecallQuestionAnswered: (
    outcome: RecallPromptAnswerOutcome
  ) => void
  readonly onReplaceCurrentRecallCard: (card: RecallCard) => void
  readonly onRecallFatalError: (message: string) => void
  readonly onConfirmLeaveRecall: () => void
}) {
  if (card.variant === 'mcq') {
    return (
      <RecallSessionChrome notebookName={card.payload.notebookName}>
        <RecallMcqStage
          key={card.payload.recallPromptId}
          payload={card.payload}
          choicesGuidanceRowBudget={10}
          inputBlockedRef={submittingRef}
          onRecallQuestionAnswered={onRecallQuestionAnswered}
          onReplaceCurrentRecallCard={onReplaceCurrentRecallCard}
          onRecallFatalError={onRecallFatalError}
          onConfirmLeaveRecall={onConfirmLeaveRecall}
        />
      </RecallSessionChrome>
    )
  }

  if (card.variant === 'spelling-session') {
    return (
      <RecallSessionChrome notebookName={card.payload.notebookName}>
        <SpellingRecallStage
          key={card.payload.memoryTrackerId}
          payload={card.payload}
          inputBlockedRef={submittingRef}
          onRecallQuestionAnswered={onRecallQuestionAnswered}
          onRecallFatalError={onRecallFatalError}
          onConfirmLeaveRecall={onConfirmLeaveRecall}
        />
      </RecallSessionChrome>
    )
  }

  return (
    <RecallSessionChrome>
      <JustReviewRecallStage
        payload={card.payload}
        inputBlockedRef={submittingRef}
        activeOperationAbortRef={activeOperationAbortRef}
        onRecallQuestionAnswered={onRecallQuestionAnswered}
        onRecallFatalError={onRecallFatalError}
        onConfirmLeaveRecall={onConfirmLeaveRecall}
      />
    </RecallSessionChrome>
  )
}

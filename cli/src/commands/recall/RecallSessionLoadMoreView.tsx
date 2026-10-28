import type { MutableRefObject } from 'react'
import { YesNoStagePrompt } from '../../commonUIComponents/YesNoStagePrompt.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import { RecallSessionChrome } from './RecallSessionChrome.js'

export function RecallSessionLoadMoreView({
  loadMoreFetching,
  runLoadMoreRecall,
  onLoadMoreFetchSuccess,
  onSettled,
  onAbortWithError,
  submitLoadMore,
  escapeLoadMorePrompt,
  submittingRef,
}: {
  readonly loadMoreFetching: boolean
  readonly runLoadMoreRecall: (signal: AbortSignal) => Promise<string>
  readonly onLoadMoreFetchSuccess: () => void
  readonly onSettled: (message: string) => void
  readonly onAbortWithError: (message: string) => void
  readonly submitLoadMore: (accept: boolean) => Promise<void>
  readonly escapeLoadMorePrompt: () => void
  readonly submittingRef: MutableRefObject<boolean>
}) {
  if (loadMoreFetching) {
    return (
      <RecallSessionChrome>
        <AsyncAssistantFetchStage
          spinnerLabel="Loading more…"
          runAssistantMessage={runLoadMoreRecall}
          onFetchSuccess={onLoadMoreFetchSuccess}
          onSettled={onSettled}
          onAbortWithError={onAbortWithError}
        />
      </RecallSessionChrome>
    )
  }
  return (
    <RecallSessionChrome>
      <YesNoStagePrompt
        key="load-more"
        prompt="Load more from next 3 days?"
        onAnswer={submitLoadMore}
        defaultAnswer={true}
        onCancel={escapeLoadMorePrompt}
        inputBlockedRef={submittingRef}
      />
    </RecallSessionChrome>
  )
}

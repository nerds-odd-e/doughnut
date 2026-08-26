import { useEffect, useState, type ReactElement } from 'react'
import { Text } from 'ink'
import {
  MemoryTrackerController,
  RecallPromptController,
  type AnsweredQuestion,
  type RecallPrompt,
} from 'donut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import { userVisibleSlashCommandError } from '../../userVisibleSlashCommandError.js'
import type { SpellingRecallSessionPayload } from './nextRecallCardLoad.js'
import { breadcrumbTrailFromRecalledNote } from './recallNoteContext.js'
import {
  RecallAnsweredBlockShell,
  recallAnsweredBreadcrumbText,
  recallAnsweredMarkdownToDisplayLines,
  recallAnsweredQuizOutcomeInk,
} from './recallAnsweredInkShared.js'

async function fetchSpellingRecallPrompt(
  memoryTrackerId: number,
  signal?: AbortSignal
): Promise<{ readonly recallPromptId: number; readonly stemMarkdown: string }> {
  const prompt = await runDefaultBackendJson<RecallPrompt>(() =>
    MemoryTrackerController.getRecallPrompt({
      path: { memoryTracker: memoryTrackerId },
      ...doughnutSdkOptions(signal),
    })
  )
  if (prompt.spellingQuestion == null) {
    throw new Error('Expected a spelling recall prompt from the server.')
  }
  const recallPromptId = prompt.id
  return {
    recallPromptId,
    stemMarkdown: prompt.spellingQuestion?.stem ?? '',
  }
}

export async function submitSpellingAnswer(
  recallPromptId: number,
  spellingAnswer: string,
  signal?: AbortSignal
): Promise<AnsweredQuestion> {
  return runDefaultBackendJson<AnsweredQuestion>(() =>
    RecallPromptController.answerSpelling({
      path: { recallPrompt: recallPromptId },
      body: { spellingAnswer },
      ...doughnutSdkOptions(signal),
    })
  )
}

export type SpellingRecallLoadState =
  | { readonly status: 'loading' }
  | {
      readonly status: 'ready'
      readonly recallPromptId: number
      readonly stemMarkdown: string
    }

export function useSpellingRecallPromptLoad(
  payload: SpellingRecallSessionPayload,
  onRecallFatalError: (message: string) => void
): SpellingRecallLoadState {
  const [loadState, setLoadState] = useState<SpellingRecallLoadState>({
    status: 'loading',
  })

  useEffect(() => {
    if (
      payload.recallPromptId !== undefined &&
      payload.stemMarkdown !== undefined
    ) {
      setLoadState({
        status: 'ready',
        recallPromptId: payload.recallPromptId,
        stemMarkdown: payload.stemMarkdown,
      })
      return
    }
    let cancelled = false
    const ac = new AbortController()
    ;(async () => {
      try {
        const fetched = await fetchSpellingRecallPrompt(
          payload.memoryTrackerId,
          ac.signal
        )
        if (cancelled) return
        setLoadState({
          status: 'ready',
          recallPromptId: fetched.recallPromptId,
          stemMarkdown: fetched.stemMarkdown,
        })
      } catch (err: unknown) {
        if (cancelled || ac.signal.aborted) return
        onRecallFatalError(userVisibleSlashCommandError(err))
      }
    })().catch(() => undefined)
    return () => {
      cancelled = true
      ac.abort()
    }
  }, [
    onRecallFatalError,
    payload.memoryTrackerId,
    payload.recallPromptId,
    payload.stemMarkdown,
  ])

  return loadState
}

export function recallAnsweredSpellingInk(args: {
  readonly answeredPrompt: AnsweredQuestion
  readonly contentMarkdownFallback: string
  readonly spellingAnswerDisplay: string
  readonly notebookName?: string
}): ReactElement {
  const width = resolvedTerminalWidth()
  const crumb = recallAnsweredBreadcrumbText(
    breadcrumbTrailFromRecalledNote(
      args.answeredPrompt.recalledNote,
      args.notebookName
    )
  )
  const contentMarkdown = args.contentMarkdownFallback
  const detailLines = recallAnsweredMarkdownToDisplayLines(
    contentMarkdown,
    width
  )
  const ans = args.spellingAnswerDisplay
  const correct = args.answeredPrompt.answer.correct === true
  return (
    <RecallAnsweredBlockShell>
      <Text>{crumb}</Text>
      {detailLines.map((line, i) => (
        <Text key={i}>{line.length > 0 ? line : ' '}</Text>
      ))}
      <Text>{`Your answer: ${ans}`}</Text>
      {recallAnsweredQuizOutcomeInk(correct)}
    </RecallAnsweredBlockShell>
  )
}

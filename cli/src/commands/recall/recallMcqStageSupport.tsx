import { Text } from 'ink'
import type { ReactElement } from 'react'
import {
  RecallPromptController,
  type AnsweredQuestion,
  type QuestionContestResult,
  type RecallPrompt,
} from 'donut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { resolvedTerminalWidth } from '../../terminalColumns.js'
import { breadcrumbTrailFromRecalledNote } from './recallNoteContext.js'
import {
  RecallAnsweredBlockShell,
  recallAnsweredBreadcrumbText,
  recallAnsweredMarkdownToDisplayLines,
  recallAnsweredQuizOutcomeInk,
} from './recallAnsweredInkShared.js'
import { numberedMcqMarkdownLinesForTerminal } from './numberedMcqMarkdownLines.js'
import {
  recallMcqPayloadFromRecallPrompt,
  type RecallMcqCardPayload,
} from './nextRecallCardLoad.js'

const CONTEST_REJECTED_FALLBACK =
  'Contest was not accepted. Please answer the question.'

export function recallAnsweredMcqInk(args: {
  readonly answeredPrompt: AnsweredQuestion
  readonly stem: string
  readonly choices: readonly string[]
  readonly selectedChoiceIndex: number
  readonly notebookName: string
}): ReactElement {
  const width = resolvedTerminalWidth()
  const crumb = recallAnsweredBreadcrumbText(
    breadcrumbTrailFromRecalledNote(
      args.answeredPrompt.recalledNote,
      args.notebookName
    )
  )
  const correct = args.answeredPrompt.answer.correct === true
  const fromMcq = args.answeredPrompt.mcq?.correctAnswerIndex
  const correctChoiceIndex =
    fromMcq !== undefined && fromMcq !== null
      ? fromMcq
      : correct && args.answeredPrompt.answer.choiceIndex !== undefined
        ? args.answeredPrompt.answer.choiceIndex
        : undefined

  const stemLines = recallAnsweredMarkdownToDisplayLines(args.stem, width)
  const listLines = numberedMcqMarkdownLinesForTerminal(args.choices, width)
  const sel = args.selectedChoiceIndex

  const choiceLineColor = (itemIndex: number): undefined | 'green' | 'red' => {
    if (correctChoiceIndex !== undefined && itemIndex === correctChoiceIndex) {
      return 'green'
    }
    if (!correct && itemIndex === sel) {
      return 'red'
    }
    return
  }

  return (
    <RecallAnsweredBlockShell>
      <Text>{crumb}</Text>
      {stemLines.map((line, i) => (
        <Text key={`st-${i}`}>{line.length > 0 ? line : ' '}</Text>
      ))}
      {listLines.map((line, i) => (
        <Text key={`ch-${i}`} color={choiceLineColor(line.itemIndex)}>
          {line.text}
        </Text>
      ))}
      {recallAnsweredQuizOutcomeInk(correct)}
    </RecallAnsweredBlockShell>
  )
}

type ContestMcqOutcome =
  | { outcome: 'replaced'; payload: RecallMcqCardPayload }
  | { outcome: 'rejected'; message: string }

/** Contest then regenerate, or rejected outcome with a user-visible message. */
export async function contestAndRegenerateMcq(
  memoryTrackerId: number,
  currentRecallPromptId: number,
  signal?: AbortSignal
): Promise<ContestMcqOutcome> {
  const contestResult = await runDefaultBackendJson<QuestionContestResult>(() =>
    RecallPromptController.contest({
      path: { recallPrompt: currentRecallPromptId },
      ...doughnutSdkOptions(signal),
    })
  )
  if (contestResult.rejected === true) {
    const message = contestResult.advice?.trim() || CONTEST_REJECTED_FALLBACK
    return { outcome: 'rejected', message }
  }
  const regenerated = await runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.regenerate({
      path: { recallPrompt: currentRecallPromptId },
      body: contestResult,
      ...doughnutSdkOptions(signal),
    })
  )
  const mapped = recallMcqPayloadFromRecallPrompt(memoryTrackerId, regenerated)
  if (mapped === null) {
    throw new Error('Regenerated recall prompt is not a pending MCQ.')
  }
  return { outcome: 'replaced', payload: mapped }
}

export async function submitMcqAnswer(
  recallPromptId: number,
  choiceIndex: number,
  signal?: AbortSignal
): Promise<AnsweredQuestion> {
  return runDefaultBackendJson<AnsweredQuestion>(() =>
    RecallPromptController.answer({
      path: { recallPrompt: recallPromptId },
      body: { choiceIndex },
      ...doughnutSdkOptions(signal),
    })
  )
}

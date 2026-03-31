import {
  MemoryTrackerController,
  RecallPromptController,
  type QuestionContestResult,
  type RecallPrompt,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'

export type RecallMcqCardPayload = {
  readonly memoryTrackerId: number
  readonly recallPromptId: number
  readonly stem: string
  readonly choices: readonly string[]
  readonly notebookTitle?: string
}

const CONTEST_REJECTED_FALLBACK =
  'Contest was not accepted. Please answer the question.'

export type ContestMcqOutcome =
  | { outcome: 'replaced'; payload: RecallMcqCardPayload }
  | { outcome: 'rejected'; message: string }

function firstPendingMcq(prompts: RecallPrompt[]): RecallPrompt | undefined {
  return prompts.find((p) => p.questionType === 'MCQ' && p.answer == null)
}

export function recallMcqPayloadFromRecallPrompt(
  memoryTrackerId: number,
  notebookTitle: string | undefined,
  prompt: RecallPrompt
): RecallMcqCardPayload | null {
  if (prompt.questionType !== 'MCQ' || prompt.answer != null) return null
  const mq = prompt.multipleChoicesQuestion
  const choices = mq?.f1__choices
  if (choices === undefined || choices.length === 0) return null
  return {
    memoryTrackerId,
    recallPromptId: prompt.id,
    stem: mq?.f0__stem?.trim() ?? '',
    choices,
    notebookTitle,
  }
}

/**
 * If this due memory tracker has a pending MCQ (existing or from askAQuestion), return it;
 * otherwise null so the session can show just-review instead.
 */
export async function tryLoadMcqPayload(
  memoryTrackerId: number,
  notebookTitle: string | undefined,
  existingPrompts: RecallPrompt[],
  signal?: AbortSignal
): Promise<RecallMcqCardPayload | null> {
  let mcqPrompt = firstPendingMcq(existingPrompts)
  if (mcqPrompt === undefined) {
    try {
      const asked = await runDefaultBackendJson<RecallPrompt>(() =>
        MemoryTrackerController.askAQuestion({
          path: { memoryTracker: memoryTrackerId },
          ...doughnutSdkOptions(signal),
        })
      )
      if (asked.questionType === 'MCQ' && asked.answer == null) {
        mcqPrompt = asked
      }
    } catch {
      // No quiz (e.g. OpenAI off): same as web Quiz.vue → just-review path.
    }
  }
  if (mcqPrompt === undefined) return null
  return recallMcqPayloadFromRecallPrompt(
    memoryTrackerId,
    notebookTitle,
    mcqPrompt
  )
}

/** Contest then regenerate, or rejected outcome with a user-visible message. */
export async function contestAndRegenerateMcq(
  memoryTrackerId: number,
  notebookTitle: string | undefined,
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
  const mapped = recallMcqPayloadFromRecallPrompt(
    memoryTrackerId,
    notebookTitle,
    regenerated
  )
  if (mapped === null) {
    throw new Error('Regenerated recall prompt is not a pending MCQ.')
  }
  return { outcome: 'replaced', payload: mapped }
}

export async function submitMcqAnswer(
  recallPromptId: number,
  choiceIndex: number,
  signal?: AbortSignal
): Promise<RecallPrompt> {
  return runDefaultBackendJson<RecallPrompt>(() =>
    RecallPromptController.answerQuiz({
      path: { recallPrompt: recallPromptId },
      body: { choiceIndex },
      ...doughnutSdkOptions(signal),
    })
  )
}

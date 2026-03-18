import {
  MemoryTrackerController,
  RecallsController,
  RecallPromptController,
  type MemoryTrackerLite,
  type RecallPrompt,
} from 'doughnut-api'
import { runWithDefaultBackendClient } from './accessToken.js'

function getTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone
}

export async function recallStatus(): Promise<string> {
  const result = await runWithDefaultBackendClient(() =>
    RecallsController.recalling({
      query: { timezone: getTimezone(), dueindays: 0 },
    })
  )
  const count = result.data?.toRepeat?.length ?? 0
  if (count === 1) {
    return '1 note to recall today'
  }
  return `${count} notes to recall today`
}

export type RecallNextResult =
  | { type: 'none'; message: string }
  | {
      type: 'just-review'
      memoryTrackerId: number
      title: string
      details?: string
    }
  | {
      type: 'mcq'
      recallPromptId: number
      stem: string
      choices: string[]
    }
  | {
      type: 'spelling'
      recallPromptId: number
      stem: string
    }

export async function recallNext(dueindays = 0): Promise<RecallNextResult> {
  const result = await runWithDefaultBackendClient(() =>
    RecallsController.recalling({
      query: { timezone: getTimezone(), dueindays },
    })
  )
  const toRepeat = result.data?.toRepeat ?? []
  const first = toRepeat[0] as MemoryTrackerLite | undefined
  if (!first?.memoryTrackerId) {
    return { type: 'none', message: '0 notes to recall today' }
  }

  const questionResult = await runWithDefaultBackendClient(() =>
    MemoryTrackerController.askAQuestion({
      path: { memoryTracker: first.memoryTrackerId },
    })
  )
  const prompt = questionResult.data
  if (prompt?.questionType === 'MCQ' && prompt.multipleChoicesQuestion) {
    const mcq = prompt.multipleChoicesQuestion
    const stem = mcq.f0__stem ?? ''
    const choices = mcq.f1__choices ?? []
    return {
      type: 'mcq',
      recallPromptId: prompt.id,
      stem,
      choices,
    }
  }
  if (prompt?.questionType === 'SPELLING') {
    const stem = prompt.spellingQuestion?.stem ?? ''
    return {
      type: 'spelling',
      recallPromptId: prompt.id,
      stem,
    }
  }

  const trackerResult = await runWithDefaultBackendClient(() =>
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: first.memoryTrackerId },
    })
  )
  const title = trackerResult.data?.note?.noteTopology?.title ?? 'Untitled note'
  const details = trackerResult.data?.note?.details
  return {
    type: 'just-review',
    memoryTrackerId: first.memoryTrackerId,
    title,
    details,
  }
}

export async function markAsRecalled(
  memoryTrackerId: number,
  successful: boolean
): Promise<void> {
  await runWithDefaultBackendClient(() =>
    MemoryTrackerController.markAsRecalled({
      path: { memoryTracker: memoryTrackerId },
      query: { successful },
    })
  )
}

export async function answerQuiz(
  recallPromptId: number,
  choiceIndex: number,
  thinkingTimeMs?: number
): Promise<{ correct: boolean }> {
  const result = await runWithDefaultBackendClient(() =>
    RecallPromptController.answerQuiz({
      path: { recallPrompt: recallPromptId },
      body: { choiceIndex, ...(thinkingTimeMs != null && { thinkingTimeMs }) },
    })
  )
  const correct = result.data?.answer?.correct ?? false
  return { correct }
}

export async function answerSpelling(
  recallPromptId: number,
  spellingAnswer: string,
  thinkingTimeMs?: number
): Promise<{ correct: boolean }> {
  const result = await runWithDefaultBackendClient(() =>
    RecallPromptController.answerSpelling({
      path: { recallPrompt: recallPromptId },
      body: {
        spellingAnswer,
        ...(thinkingTimeMs != null && { thinkingTimeMs }),
      },
    })
  )
  const correct = result.data?.answer?.correct ?? false
  return { correct }
}

function recallPromptToResult(prompt: RecallPrompt): RecallNextResult | null {
  if (prompt.questionType === 'MCQ' && prompt.multipleChoicesQuestion) {
    const mcq = prompt.multipleChoicesQuestion
    return {
      type: 'mcq',
      recallPromptId: prompt.id,
      stem: mcq.f0__stem ?? '',
      choices: mcq.f1__choices ?? [],
    }
  }
  if (prompt.questionType === 'SPELLING') {
    const stem = prompt.spellingQuestion?.stem ?? ''
    return { type: 'spelling', recallPromptId: prompt.id, stem }
  }
  return null
}

export async function contestAndRegenerate(
  recallPromptId: number
): Promise<
  { ok: true; result: RecallNextResult } | { ok: false; message: string }
> {
  const contestResult = await runWithDefaultBackendClient(() =>
    RecallPromptController.contest({
      path: { recallPrompt: recallPromptId },
    })
  )
  const data = contestResult.data
  if (contestResult.error) {
    return {
      ok: false,
      message: contestResult.error.message ?? String(contestResult.error),
    }
  }
  if (!data) {
    return { ok: false, message: 'Contest failed' }
  }
  if (data.rejected) {
    return {
      ok: false,
      message: data.advice ?? 'Question could not be regenerated',
    }
  }
  const regenerateResult = await runWithDefaultBackendClient(() =>
    RecallPromptController.regenerate({
      path: { recallPrompt: recallPromptId },
      body: data,
    })
  )
  const regenerated = regenerateResult.data
  if (regenerateResult.error) {
    return {
      ok: false,
      message: regenerateResult.error.message ?? String(regenerateResult.error),
    }
  }
  if (!regenerated) {
    return { ok: false, message: 'Regenerate failed' }
  }
  const result = recallPromptToResult(regenerated)
  if (!result) {
    return { ok: false, message: 'Unexpected question type' }
  }
  return { ok: true, result }
}

export const recallCommandDocs = [
  {
    name: '/recall-status',
    usage: '/recall-status',
    description: 'Show how many notes to recall today',
    category: 'interactive' as const,
  },
  {
    name: '/recall',
    usage: '/recall',
    description: 'Recall all due notes in a session',
    category: 'interactive' as const,
    interactiveOnly: true,
  },
]

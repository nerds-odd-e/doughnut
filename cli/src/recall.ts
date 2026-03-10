import {
  MemoryTrackerController,
  RecallsController,
  RecallPromptController,
} from '@generated/doughnut-backend-api/sdk.gen'
import type { MemoryTrackerLite } from '@generated/doughnut-backend-api'
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
  | { type: 'has-question'; message: string }

export async function recallNext(): Promise<RecallNextResult> {
  const result = await runWithDefaultBackendClient(() =>
    RecallsController.recalling({
      query: { timezone: getTimezone(), dueindays: 0 },
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
    return {
      type: 'has-question',
      message: 'Spelling recall not yet supported in CLI. Use the web app.',
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
  choiceIndex: number
): Promise<{ correct: boolean }> {
  const result = await runWithDefaultBackendClient(() =>
    RecallPromptController.answerQuiz({
      path: { recallPrompt: recallPromptId },
      body: { choiceIndex },
    })
  )
  const correct = result.data?.answer?.correct ?? false
  return { correct }
}

export const recallCommandDocs = [
  {
    name: '/recall-status',
    usage: '/recall-status',
    description: 'Show how many notes to recall today',
    category: 'interactive' as const,
  },
  {
    name: '/recall next',
    usage: '/recall next',
    description: 'Recall next note (Just Review or MCQ)',
    category: 'interactive' as const,
  },
]

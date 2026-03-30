import {
  MemoryTrackerController,
  RecallsController,
  type DueMemoryTrackers,
  type MemoryTracker,
  type RecallPrompt,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { dueRecallQuery } from './dueRecallQuery.js'

export type RecallJustReviewPayload = {
  readonly memoryTrackerId: number
  readonly noteTitle: string
  readonly detailsMarkdown: string
  readonly notebookTitle?: string
}

function hasPendingMcq(prompts: RecallPrompt[]): boolean {
  return prompts.some((p) => p.questionType === 'MCQ' && p.answer == null)
}

export async function loadRecallJustReviewPayload(): Promise<RecallJustReviewPayload> {
  const due = await runDefaultBackendJson<DueMemoryTrackers>(() =>
    RecallsController.recalling({
      query: dueRecallQuery(0),
      ...doughnutSdkOptions(),
    })
  )
  const trackers = due.toRepeat ?? []
  if (trackers.length === 0) {
    throw new Error('No notes due for recall.')
  }
  const first = trackers[0]!
  const id = first.memoryTrackerId
  const mt = await runDefaultBackendJson<MemoryTracker>(() =>
    MemoryTrackerController.showMemoryTracker({
      path: { memoryTracker: id },
      ...doughnutSdkOptions(),
    })
  )
  if (mt.spelling) {
    throw new Error('Spelling recall is not available in the CLI yet.')
  }
  const prompts = await runDefaultBackendJson<RecallPrompt[]>(() =>
    MemoryTrackerController.getRecallPrompts({
      path: { memoryTracker: id },
      ...doughnutSdkOptions(),
    })
  )
  if (hasPendingMcq(prompts)) {
    throw new Error('Multiple-choice recall is not available in the CLI yet.')
  }
  const note = mt.note
  const noteTitle = note?.noteTopology?.title?.trim() || 'Note'
  const detailsMarkdown = (note?.details ?? '').trim()
  const notebookTitle = note?.noteTopology?.notebookTitle?.trim()
  return {
    memoryTrackerId: id,
    noteTitle,
    detailsMarkdown,
    notebookTitle,
  }
}

export async function markJustReviewRecalled(
  memoryTrackerId: number,
  successful: boolean
): Promise<void> {
  await runDefaultBackendJson(() =>
    MemoryTrackerController.markAsRecalled({
      path: { memoryTracker: memoryTrackerId },
      query: { successful },
      ...doughnutSdkOptions(),
    })
  )
}

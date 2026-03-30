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

const noNotesDueForRecallMessage = 'No notes due for recall.'

export type RecallJustReviewPayload = {
  readonly memoryTrackerId: number
  readonly noteTitle: string
  readonly detailsMarkdown: string
  readonly notebookTitle?: string
}

function hasPendingMcq(prompts: RecallPrompt[]): boolean {
  return prompts.some((p) => p.questionType === 'MCQ' && p.answer == null)
}

async function fetchRecallJustReviewPayloadOrNull(
  dueInDays = 0
): Promise<RecallJustReviewPayload | null> {
  const due = await runDefaultBackendJson<DueMemoryTrackers>(() =>
    RecallsController.recalling({
      query: dueRecallQuery(dueInDays),
      ...doughnutSdkOptions(),
    })
  )
  const trackers = due.toRepeat ?? []
  if (trackers.length === 0) return null
  const first = trackers[0]!
  const id = first.memoryTrackerId
  if (id === undefined) return null
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

/** Next due just-review card, or `null` when nothing is due in that window. */
export async function loadRecallJustReviewPayloadIfAny(
  dueInDays = 0
): Promise<RecallJustReviewPayload | null> {
  return fetchRecallJustReviewPayloadOrNull(dueInDays)
}

export async function loadRecallJustReviewPayload(): Promise<RecallJustReviewPayload> {
  const p = await fetchRecallJustReviewPayloadOrNull()
  if (p === null) throw new Error(noNotesDueForRecallMessage)
  return p
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

import { MemoryTrackerController, type MemoryTracker } from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'

/** Just-review recall card (no multiple-choice question). */
export type RecallJustReviewPayload = {
  readonly memoryTrackerId: number
  readonly noteTitle: string
  readonly detailsMarkdown: string
  readonly notebookTitle?: string
}

export function recallJustReviewPayloadFromMemoryTracker(
  mt: MemoryTracker
): RecallJustReviewPayload {
  const note = mt.note
  const noteTitle = note?.noteTopology?.title?.trim() || 'Note'
  const detailsMarkdown = (note?.details ?? '').trim()
  const notebookTitle = note?.noteTopology?.notebookTitle?.trim()
  return {
    memoryTrackerId: mt.id,
    noteTitle,
    detailsMarkdown,
    notebookTitle,
  }
}

export async function markJustReviewRecalled(
  memoryTrackerId: number,
  successful: boolean,
  signal?: AbortSignal
): Promise<void> {
  await runDefaultBackendJson(() =>
    MemoryTrackerController.markAsRecalled({
      path: { memoryTracker: memoryTrackerId },
      query: { successful },
      ...doughnutSdkOptions(signal),
    })
  )
}

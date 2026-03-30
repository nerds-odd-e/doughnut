import type { MemoryTracker } from 'doughnut-api'

/** Just-review recall card (fallback when MCQ is not available). */
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

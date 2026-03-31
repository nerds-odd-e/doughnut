import type { RecallJustReviewPayload } from './nextRecallCardLoad.js'

/** One scrollback row after a recall answer; Phases 3+ add spelling/MCQ `kind` variants. */
export type RecallAnsweredRowPayload =
  | PlainRecallAnsweredRowPayload
  | JustReviewRecallAnsweredRowPayload

type PlainRecallAnsweredRowPayload = {
  readonly kind: 'plain'
  readonly text: string
}

type JustReviewRecallAnsweredRowPayload = {
  readonly kind: 'just-review'
  readonly breadcrumbTitles: readonly string[]
  readonly detailsMarkdown: string
  readonly outcome: 'remembered' | 'reduced'
  readonly noteTitle: string
}

export function plainRecallAnsweredRow(text: string): RecallAnsweredRowPayload {
  return { kind: 'plain', text }
}

export function justReviewRecallAnsweredRow(
  payload: RecallJustReviewPayload,
  remembered: boolean
): RecallAnsweredRowPayload {
  return {
    kind: 'just-review',
    breadcrumbTitles: payload.breadcrumbTitles,
    detailsMarkdown: payload.detailsMarkdown,
    outcome: remembered ? 'remembered' : 'reduced',
    noteTitle: payload.noteTitle,
  }
}
